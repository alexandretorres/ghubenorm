package sruby;

import static gitget.Log.LOG;

import java.util.Arrays;
import java.util.Iterator;

import org.jruby.ast.*;
import org.jruby.util.KeyValuePair;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.MAssociation;
import model.MAssociationDef;
import model.MCascadeType;
import model.MClass;
import model.MColumn;
import model.MJoinColumn;
import model.MProperty;
import model.MTable;
import static sruby.VisitHasOne.daoColumn;
import static sruby.VisitHasOne.daoMTable;

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
 * and there is a match by default foreign_key, it is still an inverse. Ruby imediatelly flushes updates and 
 * always selects back the belongs_to
 * @author torres
 *
 */
public class VisitHasMany implements LateVisitor {
	/**
	 * On ruby, if you set the same FK in both sides of the association, it is not the same as setting inverseof,
	 * because the retrived object will not be "the same" in memory. However it will be the same in the database (same PK)
	 * Therefore we assume that only when the names matches OR the inverse is specified that it is a bidirectional relationship.
	 * Otherwise they are two independent relationships.
	 */
	public static final boolean ASSUME_SAMEFK_AS_INVERSE=false;
	private RubyRepo repo;
	private MClass clazz;
	private IArgumentNode node;
	private String inverseOf;
	private String[] fks=null;
	private String[] pks=null;
	private MProperty prop;
	
	static DAOInterface<MProperty> daoProp = ConfigDAO.getDAO(MProperty.class);
	public VisitHasMany(RubyRepo repo,MClass clazz,IArgumentNode node) {
		this.repo=repo;
		this.clazz = clazz;
		this.node = node;
	}
	@Override
	public boolean exec() { 
		//TODO: This is ALL WRONG! creating duplicated associations
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		String pname=Helper.getValue(nameNode); 
		
		String typeName =  JRubyInflector.getInstance().singularize(pname);
		prop=daoProp.persit(clazz.newProperty());
		prop.setName(pname);
		prop.setMax(-1);
		
		MClass type = repo.getClazzFromUnderscore(clazz,typeName);
		if (type!=null)
			prop.setTypeClass(type);
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(in);
			// search for inverse_of and other stuff
		}
		//remove properties for fks
		
		//Uma PROP so tem uma assoc no lado from(?), mas uma coluna pode ter v�rias(?)
		type = prop.getTypeClass();
		if (inverseOf!=null)
			createInverseOf();
		if (prop.getAssociation()==null && type!=null) {
			String clazz_under = JRubyInflector.getInstance().underscore(clazz.getName());
			//visit SUPERCLASS PROPERTIES
			String[] defFks = fks==null ? new String[]{JRubyInflector.getInstance().foreignKey(clazz.getName())} : fks;
			for (MProperty p:type.getProperties()) {
				// this is for belongs_to
				if (p.getName().equals(clazz_under) && !p.equals(prop) && (p.getToAssociation()==null || p.getToAssociation().getFrom()==prop)) {
					if (p.getAssociation()==null) {
						continue;
					} else if (p.getAssociation().getTo()==null || p.getAssociation().getTo()==prop) {
						p.getAssociation().setTo(prop).setNavigableTo(true);
						break;
					}
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
				
				}
			}
			
			if (prop.getAssociation()==null && prop.getToAssociation()==null)
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false);
			if (fks!=null || pks!=null) {
				createFKs();
			}
			/*MProperty inverse = prop.getTypeClass().getProperties().stream().filter(
					p->p.getName().equals(prop.getN)).findFirst().orElse(null);*/
			//não tem inversa se não especifica com inverse_of, a não ser que tenha sido especificado do outro lado

		}
		return true;
	}
	private void createInverseOf() {		
		MClass ctype = prop.getTypeClass();
		if (ctype!=null) {
			final String tmp=inverseOf;
			MProperty inverse = ctype.getProperties().stream().
					filter(p->p.getName().equals(tmp)).
					findFirst().orElse(null);

			if (inverse!=null) {
				if (inverse.getAssociation()==null) {					
					MAssociation.newMAssociation(inverse, prop).setNavigableFrom(true).setNavigableTo(true);
				} else {
					//in this case the association is in the opposite side
					inverse.getAssociation().setTo(prop).setNavigableTo(true);
				}
			}
		}		

	}
	private void createFKs() {
		MClass type = prop.getTypeClass();
		if (type==null) {
			LOG.warning("Foreing Key exist for has_one "+prop.getName()+" but Type could not be found:"+prop.getType()+".");
			return;
		}			
		MTable source = (MTable) type.findDataSource();
		if (source==null) {
			if (type.isAbstract()) {
				LOG.warning("Foreing Key exist for "+prop.getName()+" but DataSource not found for class:"+type+".");
				return; // forget about it
			}
			LOG.warning("Foreing Key exist for "+prop.getName()+" but DataSource not found for class:"+type+". Creating a table source");
			source =daoMTable.persit(
					type.newTableSource(								
							JRubyInflector.getInstance().tableize(type.getName()),type.isPersistent()));
		}
		//The property of the "other side"
		final MProperty iprop = prop.getAssociation()==null ? 
				prop.getToAssociation().getFrom() : 
				prop.getAssociation().getTo()==null ? 
						prop :  
						prop.getAssociation().getTo();
		
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
	private void visitArg(Node arg) {
		if (arg instanceof HashNode) {
			HashNode hn = (HashNode) arg;
			for (KeyValuePair<Node, Node> pair:hn.getPairs()) {				
				String name=Helper.getName(pair.getKey());
				Node valueNode = pair.getValue();					
				String value = Helper.getValue(valueNode);
				MAssociationDef def=null;
				switch (name.toLowerCase()) {
					case "class_name": 
						MClass type = repo.getClazz(clazz,value);
						prop.setType(value);
						if (type!=null)
							prop.setTypeClass(type);
						break;
					case "through": 
						// This makes the association "transient", because it is fundamentally a read only shortcut (but ruby accepts dealing with them
						// has_many :yyy <-- "real" association
						// has_many :zzz :through :yyy   <-- shortcut for many-to-many OR "collapsed" one-to-many-to-many
						prop.setTransient(true);
						break;
					case "dependent":
						switch (value) {
							case "destroy":
							case "delete":
								def = prop.getOrInitAssociationDef();
								def.setOrphanRemoval(true);
								def.addCascade(MCascadeType.REMOVE);
								break;
							
						}
						break;
					case "autosave":
						if (value==null || "true".equals(value)) {
							def = prop.getOrInitAssociationDef();
							def.addCascade(MCascadeType.PERSIST);
						}
						break;
						
					case "primary_key":	
						this.pks = value.split(",");					
						break;
					case "foreign_key":				
						this.fks = value.split(",");
						break;
					case "inverse_of": 
						this.inverseOf = value;
						break;
					case "as":
						/* TODO: :as can be implemented using DiscriminatorColumn applied to one end of the 
						 * association (the belongs to side has the column). The :as declares the "global name"
						 * of the polimorphic association. The belongs to name refers to this name, instead of the
						 * class, because many classes will be in the other side. The association type may be
						 * something like "Object"
						 * 
						 */
						break;
						
				}
			}
		}
	}
	public int getOrder() {
		return 1;
	};
}
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