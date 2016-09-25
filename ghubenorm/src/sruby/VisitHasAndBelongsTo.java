package sruby;

import static gitget.Log.LOG;

import java.util.Iterator;

import org.jruby.ast.*;
import org.jruby.util.KeyValuePair;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.MAssociation;
import model.MAssociationDef;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MColumnMapping;
import model.MJoinColumn;
import model.MProperty;
import model.MTable;

/**
 * 
 * :class_name
 * 
 * Specify the class name of the association. Use it only if that name can't be
 * inferred from the association name. So has_and_belongs_to_many :projects will
 * by default be linked to the Project class, but if the real class name is
 * SuperProject, you'll have to specify it with this option. 
 * 
 * :join_table
 * 
 * Specify the name of the join table if the default based on lexical order
 * isn't what you want. WARNING: If you're overwriting the table name of either
 * class, the table_name method MUST be declared underneath any
 * has_and_belongs_to_many declaration in order to work. 
 * 
 * :foreign_key
 * 
 * Specify the foreign key used for the association. By default this is guessed
 * to be the name of this class in lower-case and �_id� suffixed. So a Person
 * class that makes a has_and_belongs_to_many association to Project will use
 * �person_id� as the default :foreign_key. 
 * 
 * :association_foreign_key
 * 
 * Specify the foreign key used for the association on the receiving side of the
 * association. By default this is guessed to be the name of the associated
 * class in lower-case and �_id� suffixed. So if a Person class makes a
 * has_and_belongs_to_many association to Project, the association will use
 * �project_id� as the default :association_foreign_key. 
 * 
 * :readonly
 * 
 * If true, all the associated objects are readonly through the association.
 * 
 * :validate
 * 
 * If false, don't validate the associated objects when saving the parent
 * object. true by default. 
 * 
 * :autosave
 * 
 * If true, always save the associated objects or destroy them if marked for
 * destruction, when saving the parent object. If false, never save or destroy
 * the associated objects. By default, only save associated objects that are new
 * records.
 * 
 * Table name rule: concatenate the association names in alphabetic order. Notice that Ruby4 also removes 
 * redundant names. This may not be so important after all (default behavior is not modeled by ENORM)
 * @author torres
 *
 */
public class VisitHasAndBelongsTo implements LateVisitor {
	private RubyRepo repo;
	private MClass clazz;
	private IArgumentNode node;
	static DAOInterface<MProperty> daoProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	MTable tab=null;
	private String[] fks=null;
	private String[] assoc_fks=null;
	public VisitHasAndBelongsTo(RubyRepo repo, MClass clazz, IArgumentNode node) {
		this.repo = repo;
		this.clazz = clazz;
		this.node = node;
	}

	@Override
	public boolean exec() {
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		String pname = Helper.getValue(nameNode);
		String typeName = JRubyInflector.getInstance().singularize(pname);
		MProperty prop=daoProp.persit(clazz.newProperty());
		prop.setName(pname);
		prop.setMax(-1);
		MClass type = repo.getClazzFromUnderscore(typeName);
		if (type!=null)
			prop.setTypeClass(type);
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(prop,in);
			// search for inverse_of and other stuff
		}
		//-------
		
		type = prop.getTypeClass();
		// seek for the association table
		if (tab==null && type!=null) {
			MTable t1 = clazz.getPersistence().getMainTable();
			MTable t2 = type.getPersistence().getMainTable();
			if (t1!=null && t2!=null) {
				String tabName = JRubyInflector.getInstance().deriveJoinTable(t1.getName(), t2.getName());
				tab = repo.getTable(tabName);
				if (tab!=null) {
					MAssociationDef def = prop.getOrInitAssociationDef();
					def.setDataSource(tab);
				}
			}
		}
		if (tab!=null) {
			createFKs(prop);
		}
		if (prop.getAssociation()==null && type!=null) {
			String clazz_under = JRubyInflector.getInstance().underscore(JRubyInflector.getInstance().pluralize(clazz.getName()));
			for (MProperty p:type.getProperties()) {				
				if (p.getName().equals(clazz_under) && !p.equals(prop)) {
					if (p.getAssociation()==null) {
						MAssociation.newMAssociation(p,prop).
						setNavigableFrom(true).
						setNavigableTo(true);
						break;
					} else if (p.getAssociation().getTo()==null) {
						p.getAssociation().setTo(prop).setNavigableTo(true);
						break;
					}
				} 
			}
			if (prop.getAssociation()==null && prop.getToAssociation()==null) {
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false).setMax(-1);
			}
			/*MProperty inverse = prop.getTypeClass().getProperties().stream().filter(
					p->p.getName().equals(prop.getN)).findFirst().orElse(null);*/
			//n�o tem inversa se n�o especifica com inverse_of, a n�o ser que tenha sido especificado do outro lado

		}
		return true;
	}
	public static MColumn getOrCreatePKColumn(MProperty idProp) {
		MColumnDefinition ret = idProp.getColumnDef();
		if (ret==null) {
			MColumn decoyId = daoColumn.persit(MColumn.newMColumn().setTable(idProp.getParent().getPersistence().getMainTable())).
					setName(idProp.getName());
			idProp.setColumnMapping(MColumnMapping.newMColumnMapping(decoyId));
			return decoyId;
		} else 
			return ret.getColumn();
	}
	private void createFKs(MProperty prop) {
		if (fks==null && assoc_fks==null)
			return;  // does not define join columns if they were not mapped explicitly.
		MAssociationDef def = prop.getOrInitAssociationDef();
		int len = fks==null ?  1  : fks.length;
		//MClass parent = prop.getParent();		
		
		for (int i=0;i<len;i++) {
			String fk = fks==null ? JRubyInflector.instance.foreignKey(clazz.getName()) : fks[i];
			MJoinColumn jc = def.findJoinColumn(fk);
			MColumn col = tab.findColumn(fk);
			if (col==null) {								
				col =  daoColumn.persit(tab.addColumn().setName(fk));		
			}
			if (jc==null) {
				jc=def.newJoingColumn(col);				
			}
			if (clazz.getPK().isEmpty())
				clazz.newPKProperty().setName("<id>").setType("integer"); 	
			if (clazz.getPK().size()>i) {
				MColumnDefinition idef = getOrCreatePKColumn(clazz.getPK().get(i));
				jc.setInverse(idef);
			}
			
		}
		len = assoc_fks==null ? 1 : assoc_fks.length;
		MClass tclass = prop.getTypeClass();
		if (tclass!=null) {
			for (int i=0;i<len;i++) {
				String fk = assoc_fks==null ? JRubyInflector.instance.foreignKey(tclass.getName()) : assoc_fks[i];
				MJoinColumn jc = def.findJoinColumn(fk);
				MColumn col = tab.findColumn(fk);
				if (col==null) {								
					col =  daoColumn.persit(tab.addColumn().setName(fk));		
				}
				if (jc==null) {
					jc=def.newJoingColumn(col);				
				}	
				if (clazz.getPK().isEmpty())
					clazz.newPKProperty().setName("<id>").setType("integer");
				if (tclass.getPK().size()>i) {
					MColumnDefinition idef = getOrCreatePKColumn(tclass.getPK().get(i));				
					jc.setInverse(idef);
				}
			}
		}
		
	}
	private void visitArg(MProperty prop,Node arg) {
		if (arg instanceof HashNode) {
			HashNode hn = (HashNode) arg;
			for (KeyValuePair<Node, Node> pair:hn.getPairs()) {				
				String name=Helper.getName(pair.getKey());
				Node valueNode = pair.getValue();					
				String value = Helper.getValue(valueNode);
				MAssociationDef def=null;
				switch (name.toLowerCase()) {
					case "class_name": 
						MClass type = repo.getClazz(value);
						if (type!=null)
							prop.setTypeClass(type);
						break;
					case "join_table": 
						tab = repo.getTable(value);
						if (tab!=null) {
							def = prop.getOrInitAssociationDef();
							def.setDataSource(tab);
						}
						break;
					case "foreign_key":				
						this.fks = value.split(",");
						break;
					case "association_foreign_key":				
						this.assoc_fks = value.split(",");
						break;							
						
				}
			}
		}
	}
}
