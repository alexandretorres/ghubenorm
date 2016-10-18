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
import model.MCascadeType;
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
public class VisitHasOne extends AbstractVisitAssoc {
	public VisitHasOne(RubyRepo repo, MClass clazz, IArgumentNode node) {
		super(repo, clazz, node);		
	}

	public int getOrder() {
		return 1;
	}
	@Override
	protected boolean isMany() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	protected String getTypeName(String pname) {		
		return pname;
	}
	
}
