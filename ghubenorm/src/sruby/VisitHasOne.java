package sruby;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jruby.ast.*;
import org.jruby.util.KeyValuePair;

import common.LateVisitor;

import static gitget.Log.LOG;


import dao.ConfigDAO;
import dao.DAOInterface;
import model.MAssociation;
import model.MAssociationDef;
import model.MClass;
import model.MColumn;
import model.MDataSource;
import model.MJoinColumn;
import model.MPersistent;
import model.MProperty;
import model.MTable;

/**
 * 
 * Options, according to
 * http://api.rubyonrails.org/classes/ActiveRecord/Associations/ClassMethods.html :
 * 
 * :class_name
 * 
 * Specify the class name of the association. Use it only if that name can't be
 * inferred from the association name. So has_one :manager will by default be
 * linked to the Manager class, but if the real class name is Person, you'll
 * have to specify it with this option. 
 * 
 * :dependent
 * 
 * Controls what happens to the associated object when its owner is destroyed:
 * 
 * 		:destroy causes the associated object to also be destroyed
 * 
 * 		:delete causes the associated object to be deleted directly from the database
 * (so callbacks will not execute)
 * 
 * 		:nullify causes the foreign key to be set to NULL. Callbacks are not
 * executed.
 * 
 * 		:restrict_with_exception causes an exception to be raised if there is an
 * associated record
 * 
 * 		:restrict_with_error causes an error to be added to the owner if there is an
 * associated object
 * 
 * Note that :dependent option is ignored when using :through option.
 * 
 * :foreign_key
 * 
 * Specify the foreign key used for the association. By default this is guessed
 * to be the name of this class in lower-case and “_id” suffixed. So a Person
 * class that makes a has_one association will use “person_id” as the default
 * :foreign_key. 
 * 
 * :foreign_type
 * 
 * Specify the column used to store the associated object's type, if this is a
 * polymorphic association. By default this is guessed to be the name of the
 * polymorphic association specified on “as” option with a “_type” suffix. So a
 * class that defines a has_one :tag, as: :taggable association will use
 * “taggable_type” as the default :foreign_type. 
 * 
 * :primary_key
 * 
 * Specify the method that returns the primary key used for the association. By
 * default this is id. 
 * 
 * :as
 * 
 * Specifies a polymorphic interface (See belongs_to). 
 * 
 * :through
 * 
 * Specifies a Join Model through which to perform the query. Options for
 * :class_name, :primary_key, and :foreign_key are ignored, as the association
 * uses the source reflection. You can only use a :through query through a
 * has_one or belongs_to association on the join model. 
 * 
 * :source
 * 
 * Specifies the source association name used by has_one :through queries. Only
 * use it if the name cannot be inferred from the association. has_one
 * :favorite, through: :favorites will look for a :favorite on Favorite, unless
 * a :source is given. 
 * 
 * :source_type
 * 
 * Specifies type of the source association used by has_one :through queries
 * where the source association is a polymorphic belongs_to. 
 * 
 * :validate
 * 
 * When set to true, validates new objects added to association when saving the
 * parent object. false by default. If you want to ensure associated objects are
 * revalidated on every update, use validates_associated. 
 * 
 * :autosave
 * 
 * If true, always save the associated object or destroy it if marked for
 * destruction, when saving the parent object. If false, never save or destroy
 * the associated object. By default, only save the associated object if it's a
 * new record.
 * 
 * Note that
 * ActiveRecord::NestedAttributes::ClassMethods#accepts_nested_attributes_for
 * sets :autosave to true. 
 * 
 * :inverse_of
 * 
 * Specifies the name of the belongs_to association on the associated object
 * that is the inverse of this has_one association. Does not work in combination
 * with :through or :as options. See ActiveRecord::Associations::ClassMethods's
 * overview on Bi-directional associations for more detail. 
 * 
 * :required
 * 
 * When set to true, the association will also have its presence validated. This
 * will validate the association itself, not the id. You can use :inverse_of to
 * avoid an extra query during validation.
 * 
 * 
 * @author torres
 *
 */
public class VisitHasOne implements LateVisitor {
	//public static final VisitBelongsTo instance = new VisitBelongsTo();
	// arguments:-----------
	private RubyRepo repo;
	private MClass clazz;
	private IArgumentNode node;
	// values....
	private String pname;
	private MClass type;
	private String typeName;
	private MProperty prop;
	private String[] fks=null;
	private String[] pks=null;
	private String inverseOf;
	
	static DAOInterface<MProperty> daoProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	public VisitHasOne(RubyRepo repo,MClass clazz,IArgumentNode node) {
		this.repo=repo;
		this.clazz = clazz;
		this.node = node;
	}
	
	public boolean exec() {
		
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		//MProperty prop = clazz.newProperty();
		pname=Helper.getValue(nameNode); 
	
		/*
		*/
		this.type = repo.getClazzFromUnderscore(pname);
		// collect data
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(in);
			// search for inverse_of and other stuff
		}
		// has_one does not have a column in THIS side of the association. A new property must be created
		/*
		prop  = clazz.getProperties().stream().
				filter(p->p.getName().equalsIgnoreCase(pname+"_id")).
				findFirst().orElse(
						null
						);
		if (prop==null)*/;
		
		prop=daoProp.persit(clazz.newProperty());
		prop.setName(pname);
		if (typeName!=null)	
			prop.setType(typeName);
		
		if (type!=null)
			prop.setTypeClass(type);
		else
			type = prop.getTypeClass();
		
		if (inverseOf!=null)
			createInverseOf();
		//remove properties for fks

		if (prop.getAssociation()==null && type!=null) {
			String clazz_under = JRubyInflector.getInstance().underscore(clazz.getName());
			for (MProperty p:type.getProperties()) {
				// this is for has_many in the other side
				if (p.getName().equals(clazz_under) && !p.equals(prop)) {
					if (p.getAssociation()==null) {
						MAssociation.newMAssociation(prop,p).
						setNavigableFrom(true).
						setNavigableTo(true);
						break;
					} else if (p.getAssociation().getTo()==null) {
						p.getAssociation().setMax(1).setTo(prop).swap(); // The default belongs_to is "many_to_one", here we set it no one_to_one
						prop.getAssociation().setNavigableTo(true);
						break;
					}
				} 
			}
			if (prop.getAssociation()==null && prop.getToAssociation()==null)
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false); 
			

		}
		
		// Set FKs, AFTER we got the association...
		if (fks!=null || pks!=null) {
			createFKs();
		}
		return true;
		
	}
	/**
	 * has one FKs refers to the OTHER side of the relationship. The Join Column will have the column pointing out to the inverse class.
	 */
	private void createFKs() {
		if (type==null) {
			LOG.warning("Foreing Key exist for has_one "+prop.getName()+" but Type could not be found:"+prop.getType()+".");
			return;
		}			
		MTable source = (MTable) type.findDataSource();
		if (source==null) {
			LOG.warning("Foreing Key exist for "+prop.getName()+" but DataSource not found for class:"+type+". Creating a table source");
			source =daoMTable.persit(
					type.newTableSource(								
							JRubyInflector.getInstance().tableize(type.getName())));
		}
		//The property of the "other side"
		final MProperty iprop =  prop.getAssociation().getTo()==null? prop :  prop.getAssociation().getTo();
		
		MAssociationDef def = prop.getOrInitAssociationDef();
		int len = fks==null ? pks.length : fks.length;
		for (int i=0;i<len;i++) {
			
			String fk = fks==null ? null : fks[i];
			String pk = pks==null ? null : pks[i];
			
			String jfk = fk==null ? JRubyInflector.getInstance().foreignKey(clazz.getName()) : fk;
			MJoinColumn jc = def.findJoinColumn(jfk);
			MColumn col = source.findColumn(jfk);
			if (col==null) {								
				col =  daoColumn.persit(source.addColumn().setName(fk));
			} else {
				//remove property
				MProperty delProp = type.getProperties().stream().
						filter(p->p.getName().equalsIgnoreCase(fk) && !p.equals(iprop)).
						findFirst().orElse(null);
				if (delProp!=null) {
					delProp.getParent().getProperties().remove(delProp);
					
				}
			}
			if (jc==null) {
				jc=def.newJoingColumn(col);				
			}			
			if (pk!=null) {
				MTable dest = (MTable) clazz.findDataSource();
				MColumn invcol = dest.findColumn(pk);
				jc.setInverse(invcol);
			}
		}	
	}
	private void createInverseOf() {
		//MAssociationDef def = prop.getOrInitAssociationDef();
		MClass ctype = prop.getTypeClass();
		if (ctype!=null) {
			final String tmp=inverseOf;
			MProperty inverse = ctype.getProperties().stream().
					filter(p->p.getName().equals(tmp)).
					findFirst().orElse(null);

			if (inverse!=null) {
				if (inverse.getAssociation()==null) {									
					MAssociation.newMAssociation(prop, inverse).setNavigableFrom(true).setNavigableTo(true);
				} else {
					//in this case the association is in the opposite side
					inverse.getAssociation().setMax(1).setTo(prop).setNavigableTo(true);
				}
			}
		}

	}
	private void visitArg(Node arg) {
		if (arg instanceof HashNode) {
			HashNode hn = (HashNode) arg;
			
			for (KeyValuePair<Node, Node> pair:hn.getPairs()) {				
				String name=Helper.getName(pair.getKey());
				Node valueNode = pair.getValue();					
				String value = Helper.getValue(valueNode);					
								
				switch (name.toLowerCase()) {
					case "class_name": 
						typeName = value;
						
						this.type = repo.getClazz(value);
						//if (type!=null)
						//	prop.setTypeClass(type);
						break;
					case "inverse_of":
						this.inverseOf = value;
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
				}
				
			}
			
		}
	}
}
