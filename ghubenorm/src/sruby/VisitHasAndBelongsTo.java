package sruby;

import java.util.Iterator;

import org.jrubyparser.ast.HashNode;
import org.jrubyparser.ast.IArgumentNode;
import org.jrubyparser.ast.Node;

import model.MAssociation;
import model.MAssociationDef;
import model.MClass;
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
 * to be the name of this class in lower-case and “_id” suffixed. So a Person
 * class that makes a has_and_belongs_to_many association to Project will use
 * “person_id” as the default :foreign_key. 
 * 
 * :association_foreign_key
 * 
 * Specify the foreign key used for the association on the receiving side of the
 * association. By default this is guessed to be the name of the associated
 * class in lower-case and “_id” suffixed. So if a Person class makes a
 * has_and_belongs_to_many association to Project, the association will use
 * “project_id” as the default :association_foreign_key. 
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
public class VisitHasAndBelongsTo implements LateVisitor<MProperty> {
	private RubyRepo repo;
	private MClass clazz;
	private IArgumentNode node;

	public VisitHasAndBelongsTo(RubyRepo repo, MClass clazz, IArgumentNode node) {
		this.repo = repo;
		this.clazz = clazz;
		this.node = node;
	}

	@Override
	public MProperty exec() {
		Iterator<Node> it = node.getArgs().childNodes().iterator();
		Node nameNode = it.next();
		String pname = Helper.getValue(nameNode);
		String typeName = NounInflector.getInstance().singularize(pname);
		MProperty prop=clazz.newProperty();
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
		if (prop.getAssociation()==null && type!=null) {
			String clazz_under = NounInflector.getInstance().underscore(NounInflector.getInstance().pluralize(clazz.getName()));
			for (MProperty p:type.getProperties()) {
				if (p.getName().equals(clazz_under)) {
					if (p.getAssociation()==null) {
						MAssociation.newMAssociation(p,prop).
						setNavigableFrom(true).
						setNavigableTo(true);
						break;
					} else if (p.getAssociation().getTo()==null) {
						p.getAssociation().setTo(prop);
						break;
					}
				} 
			}
			if (prop.getAssociation()==null)
				MAssociation.newMAssociation(prop).setNavigableFrom(true).setNavigableTo(false);
			/*MProperty inverse = prop.getTypeClass().getProperties().stream().filter(
					p->p.getName().equals(prop.getN)).findFirst().orElse(null);*/
			//não tem inversa se não especifica com inverse_of, a não ser que tenha sido especificado do outro lado

		}
		return prop;
	}
	private void visitArg(MProperty prop,Node arg) {
		if (arg instanceof HashNode) {
			HashNode hn = (HashNode) arg;
			for (Iterator<Node> it=hn.getListNode().childNodes().iterator();it.hasNext();) {
				String value =null;
				String name=Helper.getName(it.next());
				Node valueNode = null;
				if (it.hasNext()) {
					valueNode=it.next();
					value = Helper.getValue(valueNode);
					
				}
				MAssociationDef def=null;
				switch (name.toLowerCase()) {
					case "class_name": 
						MClass type = repo.getClazz(value);
						if (type!=null)
							prop.setTypeClass(type);
						break;
					case "join_table": 
						MTable tab = repo.getTable(value);
						if (tab!=null) {
							def = prop.getOrInitAssociationDef();
							def.setDataSource(tab);
						}
						break;
				}
			}
		}
	}
}
