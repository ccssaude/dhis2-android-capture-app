package com.dhis2.data.metadata;

import android.support.annotation.NonNull;

import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.dataelement.DataElementModel;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeModel;
import org.hisp.dhis.android.core.relationship.RelationshipTypeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityModel;

import java.util.List;

import io.reactivex.Observable;


/**
 * Created by ppajuelo on 04/12/2017.
 */

public interface MetadataRepository {

    /*PROGRAMS*/
    Observable<List<ProgramModel>> getProgramModelFromEnrollmentList(List<Enrollment> enrollments);

    Observable<List<ProgramModel>> getTeiActivePrograms(String teiUid);

    Observable<ProgramModel> getProgramWithId(String programUid);

    /*TRACKED ENTITY*/

    Observable<TrackedEntityModel> getTrackedEntity(String trackedEntityUid);

    Observable<TrackedEntityInstanceModel> getTrackedEntityInstance(String teiUid);

    /*CATEGORY OPTION*/

    Observable<CategoryOptionModel> getCategoryOptionWithId(String categoryOptionId);

    /*CATEGORY OPTION COMBO*/

    Observable<CategoryOptionComboModel> getCategoryOptionComboWithId(String categoryOptionComboId);

    /*ORG UNIT*/

    Observable<OrganisationUnitModel> getOrganisationUnit(String orgUnitUid);

    Observable<OrganisationUnitModel> getTeiOrgUnit(String teiUid);

    Observable<List<OrganisationUnitModel>> getOrgUnitForOpenAndClosedDate(String currentDate);

    /*PROGRAM TRACKED ENTITY ATTRIBUTE*/

    Observable<List<ProgramTrackedEntityAttributeModel>> getProgramTrackedEntityAttributes(String programUid);

    Observable<List<TrackedEntityAttributeValueModel>> getTEIAttributeValues(String teiUid);

    Observable<List<TrackedEntityAttributeValueModel>> getTEIAttributeValues(String programUid, String teiUid);

    Observable<TrackedEntityAttributeModel> getTrackedEntityAttribute(String teAttribute);


    /*RELATIONSHIPS*/

    Observable<RelationshipTypeModel> getRelationshipType(String programUid);

    Observable<List<RelationshipTypeModel>> getRelationshipTypeList();

    //ProgramStage

    @NonNull
    Observable<ProgramStageModel> programStage(String programStageId);

    Observable<DataElementModel> getDataElement(String dataElementUid);

    /*ENROLLMENTS*/
    Observable<List<EnrollmentModel>> getTEIEnrollments(String teiUid);

    Observable<List<ProgramModel>> getTEIProgramsToEnroll(String teiUid);

}
