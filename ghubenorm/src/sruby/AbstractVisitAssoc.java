package sruby;

import static gitget.Log.LOG;

import java.util.Iterator;
import java.util.logging.Level;

import org.jruby.ast.BlockAcceptingNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.DefNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IArgumentNode;
import org.jruby.ast.Node;
import org.jruby.ast.types.INameNode;
import org.jruby.util.KeyValuePair;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import gitget.Log;
import model.FetchType;
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
	protected String sourceType;
	protected String dependent;
	protected String as;
	protected MProperty asProperty = null;
	protected boolean autosave;
	protected String autosaveValue;
	
	
	

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
		
		prop=daoProp.persist(clazz.newProperty());
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
					case "source_type":
						sourceType = value;
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
			prop.setDerived(true);
			fks=null;
			pks=null;
			inverseOf=null;
			prop.setTransient(true);
			String psource = source==null? prop.getName() : source;
			VisitThrough vt = new VisitThrough(repo,prop,through,psource,sourceType);
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
			source =daoMTable.persist(
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
				col =  daoColumn.persist(source.addColumn().setName(fk));
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
				if (dest!=null) {
					MColumn invcol = dest.findColumn(pk);
					jc.setInverse(invcol);
				}
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
	private int stack=0;
	private RubyRepo repo;
	String through;
	String source;
	MProperty prop;
	boolean executed=false;
	String sourceType = null;
	//static DAOInterface<MAssociationOverride> daoAssocOver = ConfigDAO.getDAO(MAssociationOverride.class);
	
	VisitThrough(RubyRepo repo,MProperty prop,String through,String source,String sourceType) {
		this.repo=repo;
		this.through = through;
		this.prop=prop;
		this.source=source;
		this.sourceType=sourceType;
	}
	public boolean execVisitor(MProperty p) {
		
		if (p.equals(this.prop))
			return false;
		
		VisitThrough r = repo.currentVisitors.stream().filter(o->o instanceof VisitThrough).
				map(o->(VisitThrough) o).filter(o->o.prop.equals(p)).findFirst().orElse(null);
		if (r!=null) {
			stack++;
			if (stack>6) {
				LOG.warning("Stack overflow prevented visiting through "+through+" of property "+p+ " on class "+p.getParent().getFilePath());
				return false;
			}
			boolean ret =r.exec();
			stack--;
			return ret;
		}
		
		return false;
		
	}
	public MAssociationOverride findOverride(MClass clazz,MProperty p) {
		MAssociationOverride r = 
				clazz.getOverrides().stream().filter(o->o instanceof MAssociationOverride).
				map(o->(MAssociationOverride)o).filter(ao->ao.getProperties().indexOf(p)==0).findFirst().orElse(null);
		return r;
	}
	private MAssociationOverride findOrInitOverride(MClass clazz,MProperty p) {
		if (p.equals(this.prop))
			return null;
		MAssociationOverride override = findOverride(clazz, p);
		if (override==null) {
			execVisitor(p);
			override = findOverride(clazz, p);						
		}
		return override;
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
			if (p.getName().equals(through) && !p.equals(this.prop)) {
				if (p.isDerived()) {					
					prevOverride = findOrInitOverride(clazz, p);
					
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
			MAssociationOverride ao = MAssociationOverride.newMAssociationOverride(clazz);
			
			//clazz.getOverrides().add(ao);
			//daoAssocOver.persist(ao);
			ao.getProperties().add(prop);
			
			if (prevOverride==null)
				ao.getProperties().add(tprop);
			else
				ao.getProperties().addAll(prevOverride.getProperties());
			MProperty last=ao.getProperties().get(ao.getProperties().size()-1);
			MClass type = last.getTypeClass();
			if (type==null) {
				executed=true;
				if (last.getDiscriminatorColumn()!=null && last.getType()!=null) {
					prop.setType(last.getType());
				}				
				return false;
			}
			last = type.findInheritedProperty(source);
			if (last==null)
				last =  type.findInheritedProperty(JRubyInflector.instance.singularize(source));
			MClass ctype = null;
			while (last!=null && last.isDerived()) {
				MAssociationOverride postOverride = findOrInitOverride(last.getParent(), last);
				if (postOverride==null) {
					last=null;				
				} else {
					ao.getProperties().addAll(postOverride.getProperties());
					MProperty newlast = ao.getProperties().get(ao.getProperties().size()-1);
					if (!newlast.isDerived() && newlast.getTypeClass()==null)
						ctype=last.getTypeClass();
					last=newlast;						
				}
			}
			if (last!=null) {
				if (!ao.getProperties().contains(last))
					ao.getProperties().add(last);
				
				if (last.getDiscriminatorColumn()!=null) {
					if (this.sourceType!=null)
						ctype=repo.getClazz(last.getParent(), sourceType);
					else
						prop.setType("<<polymorphic>>");
				} else if (ctype==null) {
					ctype = last.getTypeClass();
				}	
				
				prop.setTypeClass(ctype);
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
/**
 * Eager loader in ruby can be implemented by calling includes (and other two methods) over a collection
 * inside the default_scope, declared at CLASS LEVEL(!)
 * class Developer < ActiveRecord::Base {
 * 		has_many :comments
 * 		default_scope -> { includes(:comments)} // this is eager loading...
 * }
 * @param lnode
 * @return
 */

class VisitDefaultScope implements LateVisitor {
	MClass clazz;
	RubyRepo repo;
	IArgumentNode node;
	
	public VisitDefaultScope(RubyRepo repo,MClass clazz, IArgumentNode node) {
		super();
		this.repo=repo;
		this.clazz = clazz;
		this.node = node;
	}

	@Override
	public boolean exec() {
		try {
			Node itNode=null;
			if (node.getArgsNode()!=null && !node.getArgsNode().childNodes().isEmpty()) {
				itNode=node.getArgsNode().childNodes().iterator().next();
			} else {
				if (node instanceof BlockAcceptingNode)
					itNode = ((BlockAcceptingNode)node).getIterNode();
			}
			if (itNode instanceof DefNode) {
				DefNode def = (DefNode) itNode;
				Node body = def.getBodyNode();
				return doExec(body);
			}
		} catch (Exception ex) {
			Log.log(RubyRepoLoader.getCurrentRepo(),Level.INFO,"Non Fatal Error at VisitDefaultScope",ex);			
		}
		return false;
	}
	protected boolean doExec(Node body) {
		boolean ret=false;
		if (body instanceof BlockNode) {
			for (Node child:((BlockNode)body).children()) {
				if (child instanceof IArgumentNode) {						
					ret=ret || doExec(child); 
				}
			}
		}
		while (body instanceof CallNode) {
			CallNode call = (CallNode) body;
			if (isEager(call) && call.getArgsNode()!=null) {
				ret=ret||setEager(call);
			}
			body = call.getReceiverNode();
		}
		if (body instanceof FCallNode) {
			FCallNode fcall  = (FCallNode) body;
			if (isEager(fcall) && fcall.getArgsNode()!=null) {					
				ret=ret||setEager(fcall);					
			}				
		}
	
		return ret;
	}
	/**
	 * picks the association, if it exists, and sets the fetch to eager
	 * @param fcall
	 * @return
	 */
	public boolean setEager(IArgumentNode fcall) {
		if (fcall.getArgsNode().childNodes().iterator().hasNext()) {			
			Node n = fcall.getArgsNode().childNodes().iterator().next();
			if (n instanceof INameNode) {
				String rel = Helper.getValue(n);
				MProperty p = clazz.findProperty(rel); // inherited properties would be an "override"
				if (p!=null && (p.getAssociation()!=null || p.getToAssociation()!=null) ) {
					p.getOrInitAssociationDef().setFetch(FetchType.EAGER);
					return true;
				}
			}
		}
		return false;
	}
	public int getOrder() {
		return 3;
	}
	/**
	 * includes is the default method for eager loading. Preload forces separated queries, and eager_load forces
	 * an outer join
	 * @param call symbol to be evaluated
	 * @return
	 */
	public static boolean isEager(Node call) {
		String val = Helper.getValue(call);
		return (val.equals("includes") || val.equals("preload") || val.equals("eager_load"));
	}
	
	
}
