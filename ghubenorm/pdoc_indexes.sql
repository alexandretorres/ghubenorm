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



