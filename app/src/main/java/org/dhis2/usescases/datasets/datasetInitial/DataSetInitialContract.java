package org.dhis2.usescases.datasets.datasetInitial;

import org.dhis2.usescases.general.AbstractActivityContracts;
import org.hisp.dhis.android.core.category.CategoryOption;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.period.PeriodType;

import java.util.Date;
import java.util.List;

public class DataSetInitialContract {

    public enum Action {
        ACTION_CREATE,
        ACTION_UPDATE,
        ACTION_CHECK
    }

    public interface View extends AbstractActivityContracts.View {

        void setAccessDataWrite(Boolean canWrite);

        void setData(DataSetInitialModel dataSetInitialModel);

        void showOrgUnitDialog(List<OrganisationUnitModel> data);

        void showPeriodSelector(PeriodType periodType, List<DateRangeInputPeriodModel> periods);

        void showCatComboSelector(String catOptionUid, List<CategoryOption> data);

        String getDataSetUid();

        OrganisationUnitModel getSelectedOrgUnit();

        Date getSelectedPeriod();

        List<String> getSelectedCatOptions();

        String getPeriodType();

        Date getPeriodDate();
    }

    public interface Presenter extends AbstractActivityContracts.Presenter {
        void init(View view);

        void onBackClick();

        void onOrgUnitSelectorClick();

        void onReportPeriodClick(PeriodType periodType);

        void onCatOptionClick(String catOptionUid);

        void onActionButtonClick();
    }


}
