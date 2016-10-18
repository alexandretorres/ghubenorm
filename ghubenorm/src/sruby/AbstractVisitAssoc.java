package sruby;

import static gitget.Log.LOG;

import java.util.Iterator;

import org.jruby.ast.HashNode;
import org.jruby.ast.IArgumentNode;
import org.jruby.ast.Node;
import org.jruby.util.KeyValuePair;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.MAssociation;
import model.MAssociationDef;
import model.MAssociationOverride;
import model.MCascadeType;
import model.MClass;
import model.MColumn;
import model.MJoinColumn;
import model.MProperty;
import model.MTable;
//TODO: double check if it is creating duplicated associations

public abstract class AbstractVisitAssoc implements LateVisitor {
	static DAOInterface<MProperty> daoProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	// arguments:-----------
	protected RubyRepo repo;
	protected MClass clazz;
	protected IArgumentNode node;
	// values....
	protected String className;
	protected String inverseOf;
	protected String[] fks=null;
	protected String[] pks=null;
	protected MProperty prop;
	protected String through;
	protected String source;
	protected String dependent;
	protected String as;
	protected MProperty asProperty = null;
	private boolean autosave;
	private String autosaveValue;
	

	public AbstractVisitAssoc(RubyRepo repo,MClass clazz,IArgumentNode node) {
		this.repo=repo;
		this.clazz = clazz;
		this.node = node;
	}
	protected abstract boolean isMany();
	protected abstract String getTypeName(String pname);
	
	@Override
	public boolean exec() {
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		String pname=Helper.getValue(nameNode);
		
		prop=daoProp.persit(clazz.newProperty());
		prop.setName(pname);
		// collect data
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(in);
		}
		if (this.isMany())
			prop.setMax(-1);	
		MClass type=null;
		if (className!=null)	{
			type = repo.getClazz(clazz,className);
			if (type!=null)
				prop.setTypeClass(type);
			prop.setType(className);
		}
		if (type==null) {
			type = repo.getClazzFromUnderscore(clazz,getTypeName(pname));
			prop.setTypeClass(type);
		}		
		setThrough();
		if (asProperty!=null && prop.getTypeClass()==null) {
			prop.setTypeClass(asProperty.getParent());			
		}
		if (autosave) {
			if (autosaveValue==null || "true".equals(autosaveValue)) {
				MAssociationDef def = prop.getOrInitAssociationDef();
				def.addCascade(MCascadeType.PERSIST);
			}
		}
		checkDependent();
		createInverseOf(isMany());
		findInverse(isMany());
		if (prop.getAssociation()==null && prop.getToAssociation()==null)
			MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false);
		if (fks!=null || pks!=null) {
			createFKs();
		}
		if (as!=null)
			prop.getAssociation().setPolymorphicAs(as);
		return true;
	}
	protected MProperty findProperty() {
		return null;
	}
	protected void visitOtherArgs(String name,String value,Node valueNode) {
		
	}
	protected void visitArg(Node arg) {
		if (arg instanceof HashNode) {
			HashNode hn = (HashNode) arg;
			for (KeyValuePair<Node, Node> pair:hn.getPairs()) {				
				String name=Helper.getName(pair.getKey());
				Node valueNode = pair.getValue();					
				String value = Helper.getValue(valueNode);
				switch (name.toLowerCase()) {
					case "class_name": 
						className = value;
						break;
					case "through": 
						// This makes the association "transient", because it is fundamentally a read only shortcut (but ruby accepts dealing with them
						// has_many :yyy <-- "real" association
						// has_many :zzz :through :yyy   <-- shortcut for many-to-many OR "collapsed" one-to-many-to-many
						through = value;
						prop.setDerived(true);
						
						break;
					case "inverse_of":
						this.inverseOf = value;
						break;
					case "dependent":
						dependent=value;						
						break;
					case "autosave":
						autosave = true;
						autosaveValue = value;
						
						break;						
					case "primary_key":	
						this.pks = value.split(",");					
						break;
					case "foreign_key":				
						this.fks = value.split(",");
						break;
					case "required":
						// do nothing, it is a software check
						break;
					case "as":
						/* :as can be implemented using DiscriminatorColumn applied to one end of the 
						 * association (the belongs to side has the column). The :as declares the "global name"
						 * of the polimorphic association. The belongs to name refers to this name, instead of the
						 * class, because many classes will be in the other side. The association type may be
						 * something like "Object"
						 * 
						 */
						as = value;
						asProperty = repo.polymorphicProperties.get(as);
						break;
					case "source":
						source=value;
						break;
					default:
						visitOtherArgs(name.toLowerCase(),value,valueNode);
						break;
				}
			}
		}
	}
	
	protected void setThrough() {
		if (through!=null) { //ignore properties
			fks=null;
			pks=null;
			inverseOf=null;
			prop.setTransient(true);
			String psource = source==null? prop.getName() : source;
			VisitThrough vt = new VisitThrough(repo,prop,through,psource);
			repo.visitors.add(vt);
		}
	}

	
	protected void createInverseOf(boolean toMany) {
		if (inverseOf!=null && as==null && through==null) {
			MClass ctype = prop.getTypeClass();
			if (ctype!=null) {
				final String tmp=inverseOf;
				MProperty inverse = ctype.getProperties().stream().
						filter(p->p.getName().equals(tmp)).
						findFirst().orElse(null);
	
				if (inverse!=null) {
					if (inverse.getAssociation()==null) {	
						if (toMany)
							MAssociation.newMAssociation(inverse, prop).setNavigableFrom(true).setNavigableTo(true);
						else
							MAssociation.newMAssociation(prop, inverse).setNavigableFrom(true).setNavigableTo(true);
					} else {
						//in this case the association is in the opposite side
						MAssociation assoc = inverse.getAssociation().setTo(prop).setNavigableTo(true);
						if (inverse.getMax()==1 && !toMany)
							assoc.setMax(1);
					}
				}
			}
		}

	}
	protected void findInverse(boolean toMany) {
		MClass type = prop.getTypeClass();
		if (prop.getAssociation()==null && type!=null && as==null && through==null) {
			String clazz_under = JRubyInflector.getInstance().underscore(clazz.getName());
			for (MProperty p:type.getProperties()) {
				// this is for belongs_to
				if (p.getName().equals(clazz_under) && !p.equals(prop) && (p.getToAssociation()==null || p.getToAssociation().getFrom()==prop)) {
					if (p.getAssociation()==null) {
						continue;
					} else if (p.getAssociation().getTo()==null || p.getAssociation().getTo()==prop) {
						p.getAssociation().setTo(prop).setNavigableTo(true);
						if (!toMany)
							p.getAssociation().setMax(1);
						break;
					}
				}
			}
		}
	}
	protected boolean isBelongsTo() {
		return false;
	}
	/**
	 * if has...  FKs refers to the OTHER side of the relationship. The Join Column will have the column pointing out to the inverse class.
	 */	
	protected void createFKs() {
		if (fks==null && pks==null)
			return;
		MClass toType;		
		MClass fromType;
		final MProperty iprop; //The property of the "other side"				
		if (isBelongsTo()) {
			toType = prop.getTypeClass();
			fromType = clazz;
			iprop=prop;
		} else {
			toType = clazz;
			fromType = prop.getTypeClass();			
			iprop = prop.getAssociation()==null ? 
					prop.getToAssociation().getFrom() : 
					prop.getAssociation().getTo()==null ? 
							prop : 
							prop.getAssociation().getTo();
		}
		if (fromType==null) {
			LOG.warning("Foreing Key exist for association "+prop.getName()+" but Type could not be found:"+prop.getType()+".");
			return;
		}			
		MTable source = (MTable) fromType.findDataSource();
		if (source==null) {
			if (fromType.isAbstract()) {
				LOG.warning("Foreing Key exist for "+prop.getName()+" but DataSource not found for class:"+fromType+".");
				return; // forget about it
			}
			LOG.warning("Foreing Key exist for "+prop.getName()+" but DataSource not found for class:"+fromType+". Creating a table source");
			source =daoMTable.persit(
					fromType.newTableSource(								
							JRubyInflector.getInstance().tableize(fromType.getName()),fromType.isPersistent()));
		}		
		MAssociationDef def = prop.getOrInitAssociationDef();
		int len = fks==null ? pks.length : fks.length;
		for (int i=0;i<len;i++) {
			
			String fk = fks==null ? null : fks[i];
			String pk = pks==null ? null : pks[i];
			
			String jfk = fk;
			if (fk==null && toType!=null)
				jfk = JRubyInflector.getInstance().foreignKey(toType.getName());
			
			MJoinColumn jc = def.findJoinColumn(jfk);
			MColumn col = source.findColumn(jfk);
			if (col==null) {
				col =  daoColumn.persit(source.addColumn().setName(fk));
			} else {
				//remove property
				MProperty delProp = fromType.getProperties().stream().
						filter(p->p.getName().equalsIgnoreCase(fk) && !p.equals(iprop)).
						findFirst().orElse(null);
				if (delProp!=null) {
					delProp.getParent().getProperties().remove(delProp);
				}
			}
			if (jc==null) {
				jc=def.newJoingColumn(col);
			}
			if (pk!=null && toType!=null) {
				MTable dest = (MTable) toType.findDataSource();
				MColumn invcol = dest.findColumn(pk);
				jc.setInverse(invcol);
			}
		}	
	}
	protected void checkDependent() {
		if (dependent!=null && through==null) {			
			switch (dependent) {
				case "destroy":
				case "delete":
					MAssociationDef def = prop.getOrInitAssociationDef();
					def.setOrphanRemoval(true);
					def.addCascade(MCascadeType.REMOVE);
					break;		
			}
		}		
	}
}
class VisitThrough implements LateVisitor {
	private RubyRepo repo;
	String through;
	String source;
	MProperty prop;
	boolean executed=false;
	//static DAOInterface<MAssociationOverride> daoAssocOver = ConfigDAO.getDAO(MAssociationOverride.class);
	
	VisitThrough(RubyRepo repo,MProperty prop,String through,String source) {
		this.repo=repo;
		this.through = through;
		this.prop=prop;
		this.source=source;
	}
	public boolean execVisitor(MProperty p) {
		VisitThrough r = repo.currentVisitors.stream().filter(o->o instanceof VisitThrough).
				map(o->(VisitThrough) o).filter(o->o.prop.equals(p)).findFirst().orElse(null);
		if (r!=null)
			return r.exec();
		return false;
		
	}
	public MAssociationOverride findOverride(MClass clazz,MProperty p) {
		MAssociationOverride r = 
				clazz.getOverrides().stream().filter(o->o instanceof MAssociationOverride).
				map(o->(MAssociationOverride)o).filter(ao->ao.getProperties().indexOf(p)==0).findFirst().orElse(null);
		return r;
	}
	
	@Override
	public boolean exec() {
		if (executed)
			return false;
		/*MClass type = prop.getTypeClass();
		if (type==null) {
			executed=true;
			return false;
		}*/
		MClass clazz = prop.getParent();
		MProperty tprop=null;
		
		MAssociationOverride prevOverride=null;
		for (MProperty p:clazz.getAllProperties()) {
			if (p.getName().equals(through)) {
				if (p.isDerived()) {					
					prevOverride = findOverride(clazz, p);
					if (prevOverride==null) {
						execVisitor(p);
						prevOverride = findOverride(clazz, p);		
						
					}
					if (prevOverride!=null) {
						tprop=p;
					}
				} else {
					tprop=p;
				}				
				break;
			}
		}
		
		if (tprop!=null) {
			MAssociationOverride ao = new MAssociationOverride();
			clazz.getOverrides().add(ao);
			//daoAssocOver.persit(ao);
			ao.getProperties().add(prop);
			
			if (prevOverride==null)
				ao.getProperties().add(tprop);
			else
				ao.getProperties().addAll(prevOverride.getProperties());
			MClass type = ao.getProperties().get(ao.getProperties().size()-1).getTypeClass();
			if (type==null) {
				executed=true;
				return false;
			}
			MProperty last = type.findInheritedProperty(source);
			if (last==null)
				last =  type.findInheritedProperty(JRubyInflector.instance.singularize(source));
			if (last!=null) {
				ao.getProperties().add(last);
				prop.setTypeClass(last.getTypeClass());
				if (!last.isDerived() && !last.isTransient() && last.getMax()==1)
					prop.setTransient(false);
			}
			
			
		}
		executed=true;
		// TODO Auto-generated method stub
		return true;
	}
	public int getOrder() {
		return 2;
	}

}
