package sruby;

import org.jruby.ast.IArgumentNode;

import model.MClass;


/**
 * :class_name
 * 
 * Specify the class name of the association. Use it only if that name can't be
 * inferred from the association name. So has_many :products will by default be
 * linked to the Product class, but if the real class name is SpecialProduct,
 * you'll have to specify it with this option. 
 * 
 * :foreign_key
 * 
 * Specify the foreign key used for the association. By default this is guessed
 * to be the name of this class in lower-case and �_id� suffixed. So a Person
 * class that makes a has_many association will use �person_id� as the default
 * :foreign_key. 
 * 
 * :foreign_type
 * 
 * Specify the column used to store the associated object's type, if this is a
 * polymorphic association. By default this is guessed to be the name of the
 * polymorphic association specified on �as� option with a �_type� suffix. So a
 * class that defines a has_many :tags, as: :taggable association will use
 * �taggable_type� as the default :foreign_type. 
 * 
 * :primary_key
 * 
 * Specify the name of the column to use as the primary key for the association.
 * By default this is id. 
 * 
 * :dependent
 * 
 * Controls what happens to the associated objects when their owner is
 * destroyed. Note that these are implemented as callbacks, and Rails executes
 * callbacks in order. Therefore, other similar callbacks may affect the
 * :dependent behavior, and the :dependent behavior may affect other callbacks.
 * 
 * 		:destroy causes all the associated objects to also be destroyed.
 * 
 * 		:delete_all causes all the associated objects to be deleted directly from the
 * database (so callbacks will not be executed).
 * 
 * 		:nullify causes the foreign keys to be set to NULL. Callbacks are not
 * executed.
 * 
 * 		:restrict_with_exception causes an exception to be raised if there are any
 * associated records.
 * 
 * 		:restrict_with_error causes an error to be added to the owner if there are
 * any associated objects.
 * 
 * If using with the :through option, the association on the join model must be
 * a belongs_to, and the records which get deleted are the join records, rather
 * than the associated records. 
 * 
 * :counter_cache
 * 
 * This option can be used to configure a custom named :counter_cache. You only
 * need this option, when you customized the name of your :counter_cache on the
 * belongs_to association. 
 * 
 * :as
 * 
 * Specifies a polymorphic interface (See belongs_to). 
 * 
 * :through
 * 
 * Specifies an association through which to perform the query. This can be any
 * other type of association, including other :through associations. Options for
 * :class_name, :primary_key and :foreign_key are ignored, as the association
 * uses the source reflection.
 * 
 * If the association on the join model is a belongs_to, the collection can be
 * modified and the records on the :through model will be automatically created
 * and removed as appropriate. Otherwise, the collection is read-only, so you
 * should manipulate the :through association directly.
 * 
 * If you are going to modify the association (rather than just read from it),
 * then it is a good idea to set the :inverse_of option on the source
 * association on the join model. This allows associated records to be built
 * which will automatically create the appropriate join model records when they
 * are saved. (See the 'Association Join Models' section above.) 
 * 
 * :source
 * 
 * Specifies the source association name used by has_many :through queries. Only
 * use it if the name cannot be inferred from the association. has_many
 * :subscribers, through: :subscriptions will look for either :subscribers or
 * :subscriber on Subscription, unless a :source is given. 
 * 
 * :source_type
 * 
 * Specifies type of the source association used by has_many :through queries
 * where the source association is a polymorphic belongs_to. 
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
 * records. This option is implemented as a before_save callback. Because
 * callbacks are run in the order they are defined, associated objects may need
 * to be explicitly saved in any user-defined before_save callbacks.
 * 
 * Note that accepts_nested_attributes_for sets :autosave to true. 
 * 
 * :inverse_of
 * 
 * Specifies the name of the belongs_to association on the associated object
 * that is the inverse of this has_many association. Does not work in
 * combination with :through or :as options. See
 * ActiveRecord::Associations::ClassMethods's overview on Bi-directional
 * associations for more detail.
 * 
 * --- test results:
 * 
 * The foreign_key can be used to specify the inverse on practice. Notice that if no foreign_key is specified, 
 * and there is a match by default foreign_key, it is still an inverse. Ruby immediately flushes updates and 
 * always selects back the belongs_to
 * @author torres
 *
 */
public class VisitHasMany extends AbstractVisitAssoc {
	public VisitHasMany(RubyRepo repo, MClass clazz, IArgumentNode node) {
		super(repo, clazz, node);		
	}
	
	public int getOrder() {
		return 1;
	}
	@Override
	protected boolean isMany() {		
		return true;
	}
	@Override
	protected String getTypeName(String pname) {
		return JRubyInflector.getInstance().singularize(pname);
	}
	
}

/*
 //visit SUPERCLASS PROPERTIES
			String[] defFks = fks==null ? new String[]{JRubyInflector.getInstance().foreignKey(clazz.getName())} : fks;
			
 } else if (ASSUME_SAMEFK_AS_INVERSE && p.getAssociation()!=null && p.getAssociationDef()!=null) {
					MAssociationDef def = p.getAssociationDef();
					String[] jcs = def.getJoinColumns().stream().map(jc->jc.getColumn().getName()).toArray(String[]::new);
					if (Arrays.equals(defFks, jcs)) {
						if (p.getAssociation().getTo()==null) {
							p.getAssociation().setTo(prop).setNavigableTo(true);
							break;
						} else {
							LOG.info("double TO association on "+p);
						}
					}
				
 */
/*
 * Inverso: has_many e belongs_to precisa especificar inverse para ser a mesma associa��o.
 * por�m, se voc� n�o especifica a foreign_key, ele acaba usando a mesma FK, pois o padrao � o nome da classe+id
 * Na pr�tica seria a mesma coluna.
 * No modelo ENORM existe a figura do "n�o especificado" que significa o valor default. Ent�o n�o podemos
 * definir que a FK � "o nome da classe + id" pois a FK � n�o especificada. 
 * Acredito que a associa��o ter� uma propriedade "to" APENAS no caso dela ser explicitamente bidirecional, j�
 * que o Ruby assim exige.
 * MAS!
 * "Every association will attempt to automatically find the inverse association and set the :inverse_of option 
 * heuristically (based on the association name). Most associations with standard names will be supported.
 * However, associations that contain the following options will not have their inverses set automatically..."
 */