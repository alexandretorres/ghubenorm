CREATE OR REPLACE FUNCTION "CleanRepo"(p_id integer)
  RETURNS void AS
$BODY$
declare r_id integer;
begin

select id into r_id from repo where publicid=p_id;

update massociationdef
set datasource_id=null
where datasource_id in 
(select id from mdatasource 
where repo_id=r_id);

update mclass
set superClass_id=null,source_id=null
where repo_id=r_id ;

delete from mdefinition_mcolumn
where columns_id in (
select c.id from mcolumn c join mdatasource ds on c.table_id=ds.id where ds.repo_id=r_id
);
delete from moverride_mproperty
where properties_id in (select p.id from mproperty p join mclass c on p.parent_id=c.id 
where c.repo_id=r_id);

WITH x AS (
delete from mjoincolumn
where generalization_id in (select gc.generalization_id from mclass_mgeneralization gc  join mclass c on gc.mclass_id=c.id 
where c.repo_id=r_id) returning column_id)
delete from mcolumn c
using x
where c.id=x.column_id;

WITH x AS (
delete from mjoincolumn
where associationdef_id in (select ad.id from massociationdef ad join mproperty p on ad.id = p.value_id join mclass c on p.parent_id=c.id 
where c.repo_id=r_id) returning column_id)
delete from mcolumn c
using x
where c.id=x.column_id;

WITH x AS (
delete from moverride where clazz_id in (select c.id from mclass c
where c.repo_id=r_id) returning column_id)
delete from mcolumn c
using x
where c.id=x.column_id;

delete from mcolumn
where table_id in (select ds.id from mdatasource ds where ds.repo_id=r_id);

delete from mcolumn
where id in (select distinct(discr_column) from mclass where repo_id=r_id);

WITH x AS (
delete from mclass_mgeneralization gc  
where gc.mclass_id in (select c.id from mclass c where c.repo_id=r_id) returning gc.generalization_id)
delete from mgeneralization g
using x
where g.id = x.generalization_id;

delete from mdefinition def 
where def.table_id in 
(select ds.id from mdatasource ds
where ds.repo_id=r_id);

delete from mdatasource_mdatasource dsds
where dsds.mjoinedsource_id in
(select ds.id from mdatasource ds
where ds.repo_id=r_id);

delete from mdatasource
where repo_id=r_id;

WITH x AS (
update mproperty p
set value_id=null
where p.value_id is not null
and p.parent_id in (select c.id from mclass c where c.repo_id=r_id)
returning p.value_id)
delete from massociationdef ad
using x
where ad.id=x.value_id;

WITH x AS (
delete from mproperty p
where p.parent_id in (select c.id from mclass c
where c.repo_id=r_id) returning p.id)
delete from massociation a
using x
where a.to_id=x.id;

delete from mclass
where repo_id=r_id;
end;$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;