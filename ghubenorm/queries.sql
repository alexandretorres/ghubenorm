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


--====
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