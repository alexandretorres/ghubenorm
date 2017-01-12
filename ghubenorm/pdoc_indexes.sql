--
-- Name: massociation_to_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX massociation_to_id_idx ON massociation USING btree (to_id);


--
-- Name: massociationdef_datasource_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX massociationdef_datasource_id_idx ON massociationdef USING btree (datasource_id);


--
-- Name: mclass_repo_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX mclass_repo_id_idx ON mclass USING btree (repo_id);


--
-- Name: mcolumn_table_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX mcolumn_table_id_idx ON mcolumn USING btree (table_id);


--
-- Name: mdatasource_repo_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX mdatasource_repo_id_idx ON mdatasource USING btree (repo_id);


--
-- Name: mproperty_parent_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX mproperty_parent_id_idx ON mproperty USING btree (parent_id);


--
-- Name: mproperty_typeclass_id_idx; Type: INDEX; Schema: public; Owner: pdoc; Tablespace: 
--

CREATE INDEX mproperty_typeclass_id_idx ON mproperty USING btree (typeclass_id);

-- Index: mdefinition_mcolumn_columns_id_idx

-- DROP INDEX mdefinition_mcolumn_columns_id_idx;

CREATE INDEX mdefinition_mcolumn_columns_id_idx
  ON mdefinition_mcolumn
  USING btree
  (columns_id);
-- Index: moverride_mproperty_properties_id_idx

-- DROP INDEX moverride_mproperty_properties_id_idx;

CREATE INDEX moverride_mproperty_properties_id_idx
  ON moverride_mproperty
  USING btree
  (properties_id);
  
-- Index: mjoincolumn_generalization_id_idx

-- DROP INDEX mjoincolumn_generalization_id_idx;

CREATE INDEX mjoincolumn_generalization_id_idx
  ON mjoincolumn
  USING btree
  (generalization_id);
  
-- Index: mjoincolumn_associationdef_id_idx

-- DROP INDEX mjoincolumn_associationdef_id_idx;

CREATE INDEX mjoincolumn_associationdef_id_idx
  ON mjoincolumn
  USING btree
  (associationdef_id);
-- Index: moverride_clazz_id_idx

-- DROP INDEX moverride_clazz_id_idx;

CREATE INDEX moverride_clazz_id_idx
  ON moverride
  USING btree
  (clazz_id);
-- Index: mdefinition_table_id_idx

-- DROP INDEX mdefinition_table_id_idx;

CREATE INDEX mdefinition_table_id_idx
  ON mdefinition
  USING btree
  (table_id);
-- Index: mmethod_clazz_id_idx

-- DROP INDEX mmethod_clazz_id_idx;

CREATE INDEX mmethod_clazz_id_idx
  ON mmethod
  USING btree
  (clazz_id);
  
-- Index: mimplement_fromclass_id_idx

-- DROP INDEX mimplement_fromclass_id_idx;

CREATE INDEX mimplement_fromclass_id_idx
  ON mimplement
  USING btree
  (fromclass_id);


