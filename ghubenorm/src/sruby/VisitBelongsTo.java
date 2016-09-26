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
//TODO: estes "visits" tem que ser executados depois que todas as classes forem visitadas.
/**
 * 
 * Options, according to
 * http://api.rubyonrails.org/classes/ActiveRecord/Associations/ClassMethods.html :
 * 
 * :class_name
 * 
 * Specify the class name of the association. Use it only if that name can't be
 * inferred from the association name. So belongs_to :author will by default be
 * linked to the Author class, but if the real class name is Person, you'll have
 * to specify it with this option. 
 * 
 * :foreign_key
 * 
 * Specify the foreign key used for the association. By default this is guessed
 * to be the name of the association with an �_id� suffix. So a class that
 * defines a belongs_to :person association will use �person_id� as the default
 * :foreign_key. Similarly, belongs_to :favorite_person, class_name: "Person"
 * will use a foreign key of �favorite_person_id�. 
 * 
 * :foreign_type
 * 
 * Specify the column used to store the associated object's type, if this is a
 * polymorphic association. By default this is guessed to be the name of the
 * association with a �_type� suffix. So a class that defines a belongs_to
 * :taggable, polymorphic: true association will use �taggable_type� as the
 * default :foreign_type. 
 * 
 * :primary_key
 * 
 * Specify the method that returns the primary key of associated object used for
 * the association. By default this is id. 
 * 
 * :dependent
 * 
 * If set to :destroy, the associated object is destroyed when this object is.
 * If set to :delete, the associated object is deleted without calling its
 * destroy method. This option should not be specified when belongs_to is used
 * in conjunction with a has_many relationship on another class because of the
 * potential to leave orphaned records behind. 
 * 
 * :counter_cache
 * 
 * Caches the number of belonging objects on the associate class through the use
 * of increment_counter and decrement_counter. The counter cache is incremented
 * when an object of this class is created and decremented when it's destroyed.
 * This requires that a column named #{table_name}_count (such as comments_count
 * for a belonging Comment class) is used on the associate class (such as a Post
 * class) - that is the migration for #{table_name}_count is created on the
 * associate class (such that Post.comments_count will return the count cached,
 * see note below). You can also specify a custom counter cache column by
 * providing a column name instead of a true/false value to this option (e.g.,
 * counter_cache: :my_custom_counter.) Note: Specifying a counter cache will add
 * it to that model's list of readonly attributes using attr_readonly.
 * 
 * :polymorphic
 * 
 * Specify this association is a polymorphic association by passing true. Note:
 * If you've enabled the counter cache, then you may want to add the counter
 * cache attribute to the attr_readonly list in the associated classes (e.g.
 * class Post; attr_readonly :comments_count; end). 
 * 
 * :validate
 * 
 * If false, don't validate the associated objects when saving the parent
 * object. false by default. 
 * 
 * :autosave
 * 
 * If true, always save the associated object or destroy it if marked for
 * destruction, when saving the parent object. If false, never save or destroy
 * the associated object. By default, only save the associated object if it's a
 * new record.
 * 
 * Note that accepts_nested_attributes_for sets :autosave to true. 
 * 
 * :touch
 * 
 * If true, the associated object will be touched (the updated_at/on attributes
 * set to current time) when this record is either saved or destroyed. If you
 * specify a symbol, that attribute will be updated with the current time in
 * addition to the updated_at/on attribute. 
 * 
 * :inverse_of
 * 
 * Specifies the name of the has_one or has_many association on the associated
 * object that is the inverse of this belongs_to association. Does not work in
 * combination with the :polymorphic options. See
 * ActiveRecord::Associations::ClassMethods's overview on Bi-directional
 * associations for more detail. 
 * 
 * :required
 * 
 * 
 * @author torres
 *
 */
public class VisitBelongsTo implements LateVisitor {
	//public static final VisitBelongsTo instance = new VisitBelongsTo();
	// arguments:-----------
	private RubyRepo repo;
	private MClass clazz;
	private IArgumentNode node;
	// values....
	private String pname;
	private MClass type;
	private MProperty prop;
	private String[] fks=null;
	private String inverseOf;
	
	static DAOInterface<MProperty> daoProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MTable> daoMTable = ConfigDAO.getDAO(MTable.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	public VisitBelongsTo(RubyRepo repo,MClass clazz,IArgumentNode node) {
		this.repo=repo;
		this.clazz = clazz;
		this.node = node;
	}
	
	public boolean exec() {
		
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		//MProperty prop = clazz.newProperty();
		pname=Helper.getValue(nameNode); 
		// SE FK foi definido, ai ferrou, porque pname+id pode ser outro campo nada a ver
		//clazz.getProperties().stream().filter(p->p.getName().equalsIgnoreCase(pname+"_id"));
		/*
		*/
		this.type = repo.getClazzFromUnderscore(pname);
		// collect data
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(in);
			// search for inverse_of and other stuff
		}
		
		prop  = clazz.getProperties().stream().
				filter(p->p.getName().equalsIgnoreCase(pname+"_id")).
				findFirst().orElse(
						null
						);
		if (prop==null)
			prop=daoProp.persit(clazz.newProperty());
		prop.setName(pname);
		
		if (type!=null)
			prop.setTypeClass(type);
		else
			type = prop.getTypeClass();
		// Set FKs...
		if (fks!=null) {
			createFKs();
		}
		if (inverseOf!=null)
			createInverseOf();
		//remove properties for fks

		if (prop.getAssociation()==null && type!=null) {
			String clazz_under = JRubyInflector.getInstance().underscore(JRubyInflector.getInstance().pluralize(clazz.getName()));
			for (MProperty p:type.getProperties()) {
				// this is for has_many in the other side
				if (p.getName().equals(clazz_under) && !p.equals(prop)) {
					if (p.getAssociation()==null) {
						MAssociation.newMAssociation(prop,p).
						setNavigableFrom(true).
						setNavigableTo(true);
						break;
					} else if (p.getAssociation().getTo()==null) {
						p.getAssociation().setTo(prop).swap();
						prop.getAssociation().setNavigableTo(true);
						break;
					}
				} 
			}
			if (prop.getAssociation()==null && prop.getToAssociation()==null)
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false).setMax(-1);  // inverse is many
			/*MProperty inverse = prop.getTypeClass().getProperties().stream().filter(
					p->p.getName().equals(prop.getN)).findFirst().orElse(null);*/
			//n�o tem inversa se n�o especifica com inverse_of, a n�o ser que tenha sido especificado do outro lado

		}
		return true;
		
	}
	private void createFKs() {
		MAssociationDef def = prop.getOrInitAssociationDef();
		
		//MClass parent = prop.getParent();
		
		MTable source = (MTable) clazz.findDataSource();
		if (source==null) {
			LOG.warning("Foreing Key exist but DataSource not found for class:"+clazz+". Creating a table source");
			source =daoMTable.persit(
					clazz.newTableSource(								
							JRubyInflector.getInstance().tableize(clazz.getName())));
		}
		/*
		Optional<MPersistent> x = Optional.ofNullable(parent.getPersistence());
		Optional<MDataSource> k = x.map(MPersistent::getSource);
		//TODO: Does not create table if it inherits!
		MTable source = (MTable) k.orElseGet(()->
			daoMTable.persit(
			parent.newTableSource(								
						JRubyInflector.getInstance().tableize(parent.getName())
				)));
		*/
		for (String fk:fks) {
			MJoinColumn jc = def.findJoinColumn(fk);
			MColumn col = source.findColumn(fk);
			if (col==null) {								
				col =  daoColumn.persit(source.addColumn().setName(fk));
			} else {
				//remove property
				MProperty delProp = clazz.getProperties().stream().
						filter(p->p.getName().equalsIgnoreCase(fk) && !p.equals(prop)).
						findFirst().orElse(null);
				if (delProp!=null) {
					delProp.getParent().getProperties().remove(delProp);
					
				}
			}
			if (jc==null) {
				jc=def.newJoingColumn(col);				
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
					//neste caso o inverse � ao contr�rio, definido do outro lado
					inverse.getAssociation().setTo(prop).setNavigableTo(true);
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
						this.type = repo.getClazz(value);
						//if (type!=null)
						//	prop.setTypeClass(type);
						break;
					case "inverse_of":
						this.inverseOf = value;
						break;
					case "foreign_key":				
						this.fks = value.split(",");
						break;
				}
				
			}
			
		}
	}
}
