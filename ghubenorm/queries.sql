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
--Repositorios selecionados que possuem classes:

select language,count(*) from repo r
where 
configpath is not null and
 exists (
select 1 from mclass
where repo_id=r.id)
group by language

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

