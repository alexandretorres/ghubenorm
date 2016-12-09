package model;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@Entity
@JsonIdentityInfo(generator=JSOGGenerator.class)
public class MFlat extends MDiscrminableGeneralization {

}
