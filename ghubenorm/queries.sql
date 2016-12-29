-- View: "Embeddable"

-- DROP VIEW "Embeddable";

CREATE OR REPLACE VIEW Embeddable AS 
 SELECT r.language, c.id, c.discriminator, c.filepath, c.isabstract, c.name, 
    c.packagename, c.persistent, c.superclassname, c.discr_column, c.source_id, 
    c.repo_id, c.superclass_id
   FROM mclass c, repo r
  WHERE c.persistent IS FALSE AND r.id = c.repo_id AND (EXISTS ( SELECT 1
           FROM mproperty p
          WHERE p.embedded IS TRUE AND p.typeclass_id = c.id));

ALTER TABLE Embeddable
  OWNER TO pdoc;
-- casacade delete
ALTER TABLE mcolumn
  DROP CONSTRAINT fk1fnp2gnjrvlhk2q231y20hdxq;


ALTER TABLE mcolumn
  ADD CONSTRAINT mcolumn_mcolumndefinition FOREIGN KEY (id)
      REFERENCES mcolumndefinition (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
-- Fix length of columns
-- has classes
ALTER TABLE repo
   ADD COLUMN hasclasses boolean DEFAULT false;

update repo r set hasclasses=true
where exists 
(select 1 from MClass c
where  r.id=c.repo_id
)

ALTER TABLE mcolumn
   ALTER COLUMN defaulvalue TYPE character varying(2048);
ALTER TABLE mcolumn RENAME defaulvalue  TO defaultvalue;
ALTER TABLE mcolumn
   ALTER COLUMN colummndefinition TYPE character varying(1024);
ALTER TABLE mproperty
   ALTER COLUMN name TYPE character varying(1024);
ALTER TABLE repo
   ALTER COLUMN configpath TYPE character varying(1024);

ALTER TABLE repo
   ALTER COLUMN url TYPE character varying(1024);
   
ALTER TABLE mclass
   ALTER COLUMN packageName TYPE character varying(1024);

ALTER TABLE mclass
   ALTER COLUMN superClassName TYPE character varying(1024);
ALTER TABLE mclass
   ALTER COLUMN filepath TYPE character varying(2048);
   
ALTER TABLE mcolumn RENAME colummndefinition  TO columndefinition;

-- look at http://stackoverflow.com/questions/5090858/how-do-you-change-the-character-encoding-of-a-postgres-database 
-- set LC_CTYPE
--====

--repositories per language
create or replace view language_repo as 
select (
case language 
	when 0 then 'JAVA' 
	when 1 then 'RUBY'
	when 2 then 'PYTHON'
	when 3 then 'OTHER'
	when 4 then 'UNKNOWN'
end) as language,count(*) as total,language as lid from repo 
group by language
order by language;

ALTER TABLE language_repo
  OWNER TO pdoc;

create or replace view repo_skip as
select 
(case skipreason 
	when 1 then 'Fork' 
	when 2 then 'HAS_PARENT'
	when 3 then 'PRIVATE'
	when 4 then 'Outro Erro'
	when 5 then 'UNKNOWN'					
	when 6 then 'Linguagem não definida'				
	when 7 then 'Outra linguagem'				
	when 8 then 'Nenhum arquivo no branch principal'			
	when 9 then 'Informações nulas retornadas'					
	when 10 then 'Eliminado na seleção'		
	when 11 then 'Número de arquivos muito grande'
end) as reason,
count(*),skipreason
from repo
where skipreason is not null
and skipreason>0
group by skipreason
order by skipreason;

ALTER TABLE repo_skip
  OWNER TO pdoc;
--Repositorios selecionados que possuem classes: (relevant repositories)

select language,count(*) from repo r
where 
configpath is not null and
 exists (
select 1 from mclass
where repo_id=r.id)
group by language

-- Classes persistentes por repositório , o total de classes (nem todas as classes são lidas!)
select sum(all_c.cnt), sum(per.cnt), avg(all_c.cnt), avg(per.cnt)
from
repo r join
(select repo_id,count(*) cnt
from mclass c
group by repo_id) all_c on r.id=all_c.repo_id
join
(select repo_id,count(*) cnt
from mclass c
where persistent is true
group by repo_id) per
on r.id=per.repo_id



--Nomes de classes persistentes mais comuns por linguagem:

select (case language when 0 then 'JAVA' when 1 then 'RUBY' end) as language,count(*),c.name from mclass c, repo r
where c.repo_id=r.id 
and c.persistent is true
group by r.language,c.name
having count(*)>1
order by count DESC

--Nomes de classes persistentes mais comuns

select all_.name,all_.count,java.count as java,ruby.count as ruby
from
(select count(*) as count,c.name from mclass c
where c.persistent is true
group by c.name
having count(*)>1
order by count DESC) all_ left outer join
(select count(*) as count,c.name from mclass c, repo r
where c.repo_id=r.id 
and c.persistent is true
and language=0
group by c.name
having count(*)>1
order by count DESC) java on all_.name=java.name
left outer join
(select count(*) as count,c.name from mclass c, repo r
where c.repo_id=r.id 
and c.persistent is true
and language=1
group by c.name
having count(*)>1
order by count DESC) ruby on all_.name=ruby.name
order by all_.count DESC



--Nomes de classes persistentes mais comuns em Java.

select language,count(*),c.name from mclass c, repo r
where c.repo_id=r.id 
and c.persistent is true
and language=0
group by r.language,c.name
order by count DESC

--Classes embutidas,por linguagem:

select r.language,count(*) from mclass c,repo r
where c.persistent is false
and r.id=c.repo_id
and exists (
select 1 from mproperty p 
where p.embedded is true
and p.typeclass_id=c.id
)
group by r.language

--Classes embutidas persistentes (um erro):

select r.language,r.id,r.url,c.filepath,c.name from mclass c,repo r
where c.persistent is true
and r.id=c.repo_id
and exists (
select 1 from mproperty p 
where p.embedded is true
and p.typeclass_id=c.id
)
-- Classes embutidas, totais
select language,count(*)
from (
select language,count(*)
from embeddable
group by language, repo_id
) tab
group by tab.language

--Classes embutidas problematicas (persistentes, por erro de ligação com classes com mesmo nome em pacote diferente)
SELECT r.language, c.id, c.filepath, c.isabstract,  c.name, 
    c.packagename, c.persistent, c.superclassname, c.repo_id, c.superclass_id,c2.name as refClass,c2.filepath as refPath,p.name as propname
   FROM mclass c, repo r,mproperty p,mclass c2
  WHERE c.persistent IS true AND r.id = c.repo_id 
  AND p.embedded IS TRUE AND p.typeclass_id = c.id
  and p.parent_id=c2.id;
--Propriedades que embutem classes desconhecidas (erro, QUANDO n�o s�o tipos b�sicos): 

select language,count(*) from mproperty p ,mclass c,repo r
where p.embedded is true
and p.typeclass_id is  null
and p.parent_id =c.id
and c.repo_id = r.id
group by language

select language,type from mproperty p ,mclass c,repo r
where p.embedded is true
and p.typeclass_id is  null
and p.parent_id =c.id
and c.repo_id = r.id

--How many classes have composite keys

select r.language,count(*)
from mclass c,repo r, (
select p.parent_id as id,count(*) as ctn from mproperty p 
where p.pk is true
group by p.parent_id
order by count(*) DESC) as pk
where pk.id=c.id
and r.id =c.repo_id
and ctn>1
group by r.language

--surrogate keys:

select p.parent_id as id from mproperty p 
where p.pk is true
and p.association_id is not null
group by p.parent_id

select c.name,p.parent_id,p.name
from mclass c join mproperty p on c.id=p.parent_id
where p.pk is true
and p.association_id is not null

--surrogate keys with two properties (special case)

select c.name,p1.parent_id,p1.name 
from mclass c,mproperty p1, mproperty p2, mcolumn col1,mcolumn col2
where p1.pk is true
and p2.association_id is not null
and p1.parent_id=p2.parent_id
and c.id=p1.parent_id
and p1.id<>p2.id
and p1.columndefinition_id=col1.id
and p2.columndefinition_id=col2.id
and upper(col1.name) = upper(col2.name)

--Averages (std deviation, variance, max) persistent classes per repository/language (that has persistent classes):

select r.language,avg(cls.cnt),stddev_samp(cls.cnt),var_samp(cls.cnt),max(cls.cnt) 
from repo r,
(select c.repo_id,count(*) as cnt
from mclass c
where c.persistent is true
group by repo_id) cls
where r.id=cls.repo_id
group by r.language

--Averages of embeddable classes per repository that uses embeddable classes:

select language,avg(e.cnt),max(e.cnt),stddev_samp(e.cnt) from
(select language,repo_id,count(*) as cnt from embeddable
group by language,repo_id) e
group by language

--Most common Embeddable class names

select name,count(*) from Embeddable
group by name
order by count(*) DESC

--Class names for the repositories with only ONE class, by language

select language, c.name,count(*)
from mclass c,
(select r.id,language,count(*) from repo r,mclass c
where r.id=c.repo_id
group by r.id,language
having count(*)=1) t
where c.repo_id=t.id
group by c.name,t.language
order by language DESC,count(*) DESC

--Count of Bidirectional associations: (?)

select language,count(a.*) from mproperty p 
join massociation a on association_id=a.id
join mclass c on p.parent_id=c.id
join repo r on r.id=c.repo_id
where c.persistent is true
and to_id is not null 
group by language

--Data sources with name distinct from the class (this makes no sense on ruby, due to pluralization they are distinct by default)
select language,c.name,ds.name
from mclass c join repo r on r.id=c.repo_id join mdatasource ds on ds.id=c.source_id
where c.persistent is true
and ds.dtype='MTable'
and upper(ds.name)<>upper(c.name)
and language=0

--Joined data source 
select language,c.name,ds.name
from mclass c join repo r on r.id=c.repo_id join mdatasource ds on ds.id=c.source_id
where ds.dtype<>'MTable'

-- child classes
select count(*)
from mclass
where superclass_id is not null
and persistent is true

select language,count(*)
from mclass c join repo r on r.id=c.repo_id
where superclass_id is not null
and persistent is true
group by language

select sup.name,sup.persistent,count(*)
from mclass c join repo r on r.id=c.repo_id join mclass sup on c.superclass_id=sup.id
where c.superclass_id is not null
and c.persistent is true
group by sup.name,sup.persistent
order by count(*) DESC

-- top discriminator values for child classes
select discriminatorvalue,count(*)
from mgeneralization
where discriminatorvalue is not null
group by discriminatorvalue
having count(*)>1
order by count(*) DESC
--TODO: sum with discriminator for parents at the mclass
-- preferred strategy
select dtype,count(*)
from mgeneralization
group by dtype

-- Subclasses by relevant repository
select language,r.id,coalesce(cnt,0) 
from repo r left outer join  
(select c.repo_id,count(*) as cnt
from mclass c 
where superclass_id is not null
and persistent is true
group by c.repo_id
) as subc on r.id=subc.repo_id
where configpath is not null 
and exists (
select 1 from mclass m2
where m2.repo_id=r.id)
order by coalesce(cnt,0)  DESC

-- number of repos with subclasses x total number of relevant repos

select cnt_sub,cnt_r
from
(select count(*) as cnt_sub
from 
(select distinct c.repo_id
from mclass c 
where superclass_id is not null
and persistent is true
) as s1) as srepos ,
(select count(*) as cnt_r
from repo r
where configpath is not null 
and exists (
select 1 from mclass m2
where m2.repo_id=r.id)) as repos

--many to many (bidirectional)
select count(*) 
from mproperty p1 join massociation a on p1.association_id=a.id and p1.id<>a.to_id
join mproperty p2 on a.to_id=p2.id
where
p1.max<0 and p2.max<0

--many to many (unidir 1)
select count(*) 
from mproperty p1 join massociation a on p1.association_id=a.id and a.to_id is null
join massociationdef adef on p1.value_id=adef.id
where
p1.max<0 
and adef.datasource_id is not null
-- strange association data (1)
-- ERROR: ON UNIDIRECTIONAL MANY-TO-MANY, MASSOCIATION SHOULD INDICATE IT. IN UML IT HAS A HIDDEN PROPERTY.

-- Association between the SAME properties (it is a bug. If autoassoc, should relate to another property of the same class)
select a1.id,a1.to_id,a1.max,a2.id,a2.to_id,a2.max 
from MAssociation a1 join MProperty tp on tp.id=a1.to_id join MAssociation a2 on a1.id=tp.association_id 
where a2.to_id=tp.id
and a1.id=a2.id
-- Problematic ruby associations
select c.filepath,tp.name,a1.id,a1.to_id,a1.max,a2.id,a2.to_id,a2.max 
from MAssociation a1 join MProperty tp on tp.id=a1.to_id join MAssociation a2 on a1.id=tp.association_id join mclass c on tp.parent_id=c.id
where a2.to_id=tp.id
and a1.id=a2.id
-- This checks the odd situation where a many-to-many declares joinColumns that refer to a table that inherits a PK from mapped superclass
-- In this specific situation, an override is created to distinguish the joinColumn and inversejoincolumn on java
select c.filepath,p.name,col.name,col.table_id,jtab.name, adef.*,jc.*, invClass.name from repo r join mclass c on r.id=c.repo_id join mproperty p on p.parent_id=c.id
 join massociationdef adef on  p.value_id=adef.id
 join mjoincolumn jc on jc.associationdef_id =adef.id
 join mcolumn col on col.id=jc.column_id
 join mdatasource jtab on datasource_id=jtab.id
 join moverride mo on mo.column_id = jc.inverse_id
 join mclass invClass on mo.clazz_id = invClass.id
where
language=0
and datasource_id is not null

--error check: one-to-one to association pointing to MANY
select count(to_id) from massociation 	
group by to_id
having count(to_id)>1

select count(a.to_id) ,c.id,c.filepath
from massociation a join mproperty p on p.association_id=a.id
join mclass c on c.id=p.parent_id join repo r on r.id=c.repo_id
where r.language=1
group by a.to_id,c.filepath,c.id
having count(a.to_id)>1

select a.*,p.*,c.filepath,ty.filepath
from massociation a join mproperty p on p.association_id=a.id
join mclass c on c.id=p.parent_id join repo r on r.id=c.repo_id
join mclass ty on p.typeclass_id=ty.id
where c.id=?

-- check for fk defined by the other side of relationship. Notice that assoc.to_id "should" be null, or the Fk should be defined in the opposite side?
-- This query needs some work
select cl.name as fromClass,cl.source_id as fromtable,p.name as fromProperty,col.name as colname,col.table_id as tableid,tab.name as toTable,toCl.name as toClass,assoc.id,assoc.to_id
from MProperty p join MAssociationDef def on p.value_id=def.id
join MJoinColumn jc on jc.associationdef_id = def.id
join MColumn col on jc.column_id=col.id
join MClass cl on p.parent_id=cl.id
left outer join MClass toCl on toCl.source_id = col.table_id
left outer join MDataSource tab on tab.id= col.table_id
left outer join MAssociation assoc on assoc.id=p.association_id
where cl.source_id<>col.table_id


-- association overrides

select * from MOverride o join MAssociationDef def on o.def_id=def.id
join MJoinColumn jc on jc.associationdef_id=def.id 
join MColumn toCol on toCol.id=jc.column_id
left outer join MColumn inv on jc.inverse_id=inv.id
left outer join MDataSource invTab on inv.table_id=invTab.id
where clazz_id=<num>

-- how many classes (in total) a java repo has - check for page limit of 1000 on ruby API
select r2.id,r2.name,cnt from
repo r2 join (
select r.id,count(*) as cnt from repo r join mclass c on r.id=c.repo_id
group by r.id) as res on res.id=r2.id 
where r2.language=0
order by cnt desc

-- all "MFlat" repos
SELECT CAST(r.publicid as text) as id
from mgeneralization g,mclass_mgeneralization mg,mclass c,repo r
where dtype='MFlat'
and g.id=mg.generalization_id
and mg.mclass_id=c.id
and c.repo_id = r.id
group by r.publicid

-- embedded properties with unsolved recorded type
select r.id,p.type ,count(*)
from mproperty p join mclass c on c.id=p.parent_id join repo r on r.id=c.repo_id
where p.embedded is true
and p.typeclass_id is null
and type is not null
and type not in (
'String',
'List<String>',
'Set<String>',
'Collection<String>',
'SortedSet<String>',
'Date',
'List<Date>',
'Set<Date>',
'Collection<Date>',
'Integer','int'
'Set<Integer>',
'List<Integer>',
'Set<Integer>',
'Collection<Integer>',
'List',
'Long','long',
'Set<Long>',
'List<Long>',
'Collection<Long>',
'Set<Double>',
'List<Double>',
'Collection<Double>',
'List<BigDecimal>',
'Collection<BigDecimal>',
'List<Boolean>',
'Collection<Boolean>',
'List<byte[]>',
'List<Byte>',
'Collection<Byte>',
'Set<Byte>',
'List<Character>',
'Collection<Character>',
'Set<Character>',
'List<Float>',
'Collection<Float>',
'Set<Float>',
'List<Object>',
'Collection<Object>',
'Set<Object>',
'SortedMap<String, String>',
'Map<Integer, Date>',
'Map<Integer, Integer>',
'Map<Integer, Long>',
'Map<Integer, String>',
'Map<Long, String>',
'Map<String, String>',
'Map<String, Float>',
'Map<String, Integer>',
'Map<Date, String>',
'Map<String, Object>'
)
group by p.type,r.id
order by r.id,p.type;

-- count non identified embedded classes
select sum(n) from (
select count(*) as n from (
select r.id as id,p.type ,count(*)
from mproperty p join mclass c on c.id=p.parent_id join repo r on r.id=c.repo_id
where p.embedded is true
and p.typeclass_id is null
and type is not null
and type not in (
'String',
'List<String>',
'Set<String>',
'Collection<String>',
'SortedSet<String>',
'Date',
'List<Date>',
'Set<Date>',
'Collection<Date>',
'Integer','int'
'Set<Integer>',
'List<Integer>',
'Set<Integer>',
'Collection<Integer>',
'List',
'Long','long',
'Set<Long>',
'List<Long>',
'Collection<Long>',
'Set<Double>',
'List<Double>',
'Collection<Double>',
'List<BigDecimal>',
'Collection<BigDecimal>',
'List<Boolean>',
'Collection<Boolean>',
'List<byte[]>',
'List<Byte>',
'Collection<Byte>',
'Set<Byte>',
'List<Character>',
'Collection<Character>',
'Set<Character>',
'List<Float>',
'Collection<Float>',
'Set<Float>',
'List<Object>',
'Collection<Object>',
'Set<Object>',
'SortedMap<String, String>',
'Map<Integer, Date>',
'Map<Integer, Integer>',
'Map<Integer, Long>',
'Map<Integer, String>',
'Map<Long, String>',
'Map<String, String>',
'Map<String, Float>',
'Map<String, Integer>',
'Map<Date, String>',
'Map<String, Object>'

)
group by p.type,r.id
order by r.id,p.type) as T
group by T.id) as T2
-- propriedades de classes embutidas em ruby com tipo não identificado (nulo). Poderia registrar o nome do tipo?
select r.id as id,p.type 
from mproperty p join mclass c on c.id=p.parent_id join repo r on r.id=c.repo_id
where p.embedded is true
and p.typeclass_id is null
and language=1
-- composite PKs on Ruby
select r.id,p.parent_id,count(*) cnt,c.name,c.filepath
from mproperty p join mclass c on p.parent_id=c.id join repo r on c.repo_id=r.id
where pk is true
and r.language=1
group by p.parent_id,r.id,c.name,c.filepath
having count(*)>1
-- composite PKs on ruby analysis
select sum(cnt),avg(cnt),stddev_samp(cnt),var_samp(cnt),max(cnt) from (
select r.id,count(*) as cnt
from (
select r.id,p.parent_id,count(*) cnt,c.name,c.filepath
from mproperty p join mclass c on p.parent_id=c.id join repo r on c.repo_id=r.id
where pk is true
and r.language=1
group by p.parent_id,r.id,c.name,c.filepath
having count(*)>1) as R
group by r.id) as R2
where cnt<22
-- composite PKs using EmbeddedId (?) on Java
select r.id,p.parent_id,count(*) cnt,c.name,c.filepath,t.name,t.filepath
from mproperty p join mclass c on p.parent_id=c.id and p.pk is true join repo r on c.repo_id=r.id 
	join mclass t on t.id=p.typeclass_id and t.persistent is false join mproperty p2 on p2.parent_id=t.id
where r.language=0
group by p.parent_id,r.id,c.name,c.filepath,t.id,t.name,t.filepath