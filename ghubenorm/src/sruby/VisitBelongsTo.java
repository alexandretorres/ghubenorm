package sruby;

import static gitget.Log.LOG;

import java.util.Iterator;

import org.jruby.ast.IArgumentNode;
import org.jruby.ast.Node;

import model.MAssociation;
import model.MClass;
import model.MColumn;
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
public class VisitBelongsTo extends AbstractVisitAssoc {
	
	private String pname;
	private boolean polymorphic=false;
	private String foreignType;

	public VisitBelongsTo(RubyRepo repo, MClass clazz, IArgumentNode node) {
		super(repo, clazz, node);
		// TODO Auto-generated constructor stub
	}
	public MProperty findProperty(String pname) {
		return clazz.getProperties().stream().
				filter(p->p.getName().equalsIgnoreCase(pname+"_id")).
				findFirst().orElse(
						null);
	}
	public boolean exec() {		
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		pname=Helper.getValue(nameNode); 		
		
		// collect data
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(in);			
		}			
		prop  = findProperty(pname);	
		if (prop==null)
			prop=daoProp.persist(clazz.newProperty());
		prop.setName(pname);
		MClass type=null;
		if (className!=null)	{
			type = repo.getClazz(clazz,className);
			if (type!=null)
				prop.setTypeClass(type);
			prop.setType(className);
		}
		if (type==null && !polymorphic) {
			type = repo.getClazzFromUnderscore(clazz,getTypeName(pname));
			prop.setTypeClass(type);
		}
		
		createFKs();		
		createInverseOf(false);
		//remove properties for fks
		if (polymorphic) {
			String dname = foreignType ==null? pname+"_type" : foreignType;
			
			MColumn dcolumn = null;
			MTable source = (MTable) clazz.findDataSource();
			if (source!=null) {
				dcolumn = source.findColumn(dname);
			}
			if (dcolumn==null) {
				LOG.info("creating a discriminator column "+dname+" from table "+source+" of class "+clazz );				
				dcolumn = daoColumn.persist(MColumn.newMColumn().setName(dname).setTable(source));				
			} else {
				MProperty delProp = clazz.findProperty(dname);
				if (delProp!=null && dcolumn.equals(delProp.getColumnDef())) {
					delProp.getParent().getProperties().remove(delProp);
					daoProp.remove(delProp);
				}
			}
			prop.setType("<polymorphic>"+dcolumn.getName());
			this.repo.polymorphicProperties.put(pname, prop); //TODO: packaged names !
			prop.getDiscriminatorColumn().setColumn(dcolumn);
		} else if (prop.getAssociation()==null && type!=null) {
			String clazz_under = JRubyInflector.getInstance().underscore(JRubyInflector.getInstance().pluralize(clazz.getName())); //has many
			findAssociationByName(clazz_under);
			if (prop.getAssociation()==null) {
				clazz_under = JRubyInflector.getInstance().underscore(clazz.getName()); //has One
				findAssociationByName(clazz_under);
			}
		}
		if (prop.getAssociation()==null && prop.getToAssociation()==null)
			MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false).setMax(-1);  // inverse is many by default
		
		return true;
		
	}
	private void findAssociationByName(String clazz_under) {		
		for (MProperty p:prop.getTypeClass().getProperties()) {
			// this is for has_many in the other side
			if (p.getName().equals(clazz_under) && !p.equals(prop) && (p.getToAssociation()==null || p.getToAssociation().getFrom()==prop)) {
				if (p.getAssociation()==null) { //perhaps we should not create an association until the other side have one
					/*MAssociation.newMAssociation(prop,p).
					setNavigableFrom(true).
					setNavigableTo(true);
					break;*/
				} else if (p.getAssociation().getTo()==null || p.getAssociation().getTo()==prop) {
					p.getAssociation().setTo(prop).swap();
					prop.getAssociation().setNavigableTo(true);
					break;
				}
			} 
		}
	}
	protected boolean isBelongsTo() {
		return true;
	}	
	
	@Override
	protected void visitOtherArgs(String name, String value, Node valueNode) {
		switch (name) {			
			case "polymorphic":
				polymorphic=true;						
				className="<polymorphic::"+this.pname+">";						
				break;
			case "foreign_type":
				foreignType=value;
				break;
		}
	}
	
	@Override
	protected boolean isMany() {		
		return false;
	}
	@Override
	protected String getTypeName(String pname) {		
		return pname;
	}
}
