package sruby;

import java.util.Iterator;

import org.jruby.ast.*;
import org.jruby.util.KeyValuePair;

import common.LateVisitor;
import dao.ConfigDAO;
import dao.DAOInterface;
import model.MAssociationDef;
import model.MAttributeOverride;
import model.MClass;
import model.MColumn;
import model.MColumnDefinition;
import model.MDataSource;
import model.MOverride;
import model.MProperty;
import model.MTable;

public class VisitComposedOf implements LateVisitor {
	private RubyRepo repo;
	private MClass clazz;
	private IArgumentNode node;
	private String[][] args;
	private MClass type ;
	static DAOInterface<MProperty> daoProp = ConfigDAO.getDAO(MProperty.class);
	static DAOInterface<MColumn> daoColumn = ConfigDAO.getDAO(MColumn.class);
	
	public VisitComposedOf(RubyRepo repo, MClass clazz, IArgumentNode node) {
		this.repo = repo;
		this.clazz = clazz;
		this.node = node;
	}

	public boolean exec() { 		
		if (!clazz.isPersistent())
			return true;
		//Only Tables sources on activerec. ruby
		MTable tab = (MTable) clazz.getPersistence().getSource();
		Iterator<Node> it = node.getArgsNode().childNodes().iterator();
		Node nameNode = it.next();
		String pname = Helper.getValue(nameNode);
		String typeName = pname;
		MProperty prop=daoProp.persit(clazz.newProperty().setName(pname).setMax(1));
		type = repo.getClazzFromUnderscore(typeName);
		if (type!=null)
			prop.setTypeClass(type);
		prop.setEmbedded(true);
		while (it.hasNext()) {
			Node in = it.next();
			visitArg(prop,in);
			// search for inverse_of and other stuff
		}
		if (type!=null) {
			if (args==null) { //No mapping, args is the property list
				args = new String[type.getProperties().size()][1];
				int i=0;
				for (MProperty p:type.getProperties()) {
					args[i][0] = p.getName();
					i++;
				}
			}
			for (int i=0;i<args.length;i++) {
				String[] str = args[i];
				//for (String[] str:this.args) {				
				MColumn col=null;
				if (tab!=null)
					col = tab.getColumns().stream().
						filter(c->c.getName().equalsIgnoreCase(str[0]))
						.findAny().orElse(null);
				if (col==null) { // the column may be defined by the specizalizations (like a @Column in a @MappedSuperClass
					col = daoColumn.persit( MColumn.newMColumn().setName(str[0]));
				}
				MProperty embedRef=null;
				final String delName= str[0];
				if (str.length>1) {
					embedRef = type.getProperties().stream().
						filter(p->p.getName().equalsIgnoreCase(str[1])).findAny().orElse(null);
								
				} else if (type.getProperties().size()>i) {
					embedRef = type.getProperties().get(i);
				}
				
				//--
				
				if (embedRef!=null) {
					clazz.override(MAttributeOverride.newMAttributeOverride(col,prop,embedRef));
					//remove the property
					
					MProperty delProp = clazz.getProperties().stream().
							filter(p->p.getName().equalsIgnoreCase(delName) && !p.equals(prop)).
							findFirst().orElse(null);
					if (delProp!=null) {
						delProp.getParent().getProperties().remove(delProp);						
					}
				}
			}
			for (MClass sub:clazz.getSpecializations()) {
				checkSubClass(clazz,sub,prop);
			}
		}
		return true;
	}
	/** 
	 * For overrides at abstract super-classes or overrides of overrides(?) remove property columns, fix the local overriding
	 * @param parent
	 * @param clazz
	 * @param prop
	 */
	private void checkSubClass(MClass parent,MClass clazz,MProperty prop) {
		//TODO: checks for another composed_of at the same fields?
		for (String[] str:this.args) {	
			for (Iterator<MProperty> it=clazz.getProperties().iterator();it.hasNext();) {
				MProperty p = it.next();
				if (p.getName().equals(str[0])) {					
					it.remove();
				}
			}
		}
		for (MClass sub:clazz.getSpecializations()) {
			checkSubClass(clazz,sub,prop);
		}
		/*
		MTable tab = (MTable) clazz.getPersistence().getSource();
		
		for (MColumn col:tab.getColumns()) {
			if (col.getName().equalsIgnoreCase(arg0))
		}*/
	}
	private void visitArg(MProperty prop,Node arg) {
		if (arg instanceof HashNode) {
			HashNode hn = (HashNode) arg;
			for (KeyValuePair<Node, Node> pair:hn.getPairs()) {				
				String name=Helper.getName(pair.getKey());
				Node valueNode = pair.getValue();				
		
				MAssociationDef def=null;
				switch (name.toLowerCase()) {
					case "class_name": 
						String value= Helper.getValue(valueNode);
						this.type = repo.getClazz(value);						
						if (type!=null)
							prop.setTypeClass(type);
						break;
					case "mapping": 
						ArrayNode an = (ArrayNode) valueNode;
						if (an.children().length==2 && !(an.children()[0] instanceof ArrayNode)) {
							args = new String[1][2];
							args[0] = new String[] {Helper.getValue(an.children()[0]),Helper.getValue(an.children()[1])};							
						} else {
							args = new String[an.children().length][2];
							int i=0;
							for (Node n:an.children()) {
								String[] values= Helper.getValue(n).split(",");
								args[i++] = values;					
								
								//System.out.println("value:"+value);
							}
						}
						break;
					
				}
			}
		}
	}
}
