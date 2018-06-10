package com.dhis2.data.forms;

import android.support.annotation.NonNull;

import com.dhis2.data.forms.dataentry.EnrollmentRuleEngineRepository;
import com.dhis2.data.forms.dataentry.EventsRuleEngineRepository;
import com.dhis2.data.forms.dataentry.RuleEngineRepository;
import com.dhis2.data.forms.dataentry.fields.FieldViewModel;
import com.dhis2.data.schedulers.SchedulerProvider;
import com.dhis2.utils.DateUtils;
import com.dhis2.utils.Result;
import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionHideSection;
import org.hisp.dhis.rules.models.RuleEffect;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import rx.exceptions.OnErrorNotImplementedException;
import timber.log.Timber;

import static com.dhis2.utils.Preconditions.isNull;


class FormPresenterImpl implements FormPresenter {

    @NonNull
    private final FormViewArguments formViewArguments;

    @NonNull
    private final SchedulerProvider schedulerProvider;

    @NonNull
    private final FormRepository formRepository;

    @NonNull
    private final CompositeDisposable compositeDisposable;
    @NonNull
    private final RuleEngineRepository ruleEngineRepository;

    @NonNull
    private final FlowableProcessor<String> processor;

    FormPresenterImpl(@NonNull FormViewArguments formViewArguments,
                      @NonNull SchedulerProvider schedulerProvider,
                      @NonNull BriteDatabase briteDatabase,
                      @NonNull FormRepository formRepository) {
        this.formViewArguments = formViewArguments;
        this.formRepository = formRepository;
        this.schedulerProvider = schedulerProvider;
        this.compositeDisposable = new CompositeDisposable();
        if (formViewArguments.type() == FormViewArguments.Type.ENROLLMENT)
            this.ruleEngineRepository = new EnrollmentRuleEngineRepository(briteDatabase, formRepository, formViewArguments.uid());
        else
            this.ruleEngineRepository = new EventsRuleEngineRepository(briteDatabase, formRepository, formViewArguments.uid());

        this.processor = PublishProcessor.create();
    }

    @Override
    public void onAttach(@NonNull FormView view) {
        isNull(view, "FormView must not be null");

        compositeDisposable.add(formRepository.title()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(view.renderTitle(), Timber::e));

        compositeDisposable.add(formRepository.reportDate()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .map(date -> {
                    try {
                        return DateUtils.uiDateFormat().format(DateUtils.databaseDateFormat().parse(date));
                    } catch (ParseException e) {
                        Timber.e(e, "DashboardRepository: Unable to parse date. Expected format: " +
                                DateUtils.databaseDateFormat().toPattern() + ". Input: " + date);
                        return date;
                    }
                })
                .subscribe(view.renderReportDate(), Timber::e));

        compositeDisposable.add(formRepository.incidentDate()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .filter(programModelAndDate -> programModelAndDate.val0().displayIncidentDate())
                .subscribe(view.renderIncidentDate(), Timber::e)
        );

        compositeDisposable.add(formRepository.getAllowDatesInFuture()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(program -> view.initReportDatePicker(program.selectEnrollmentDatesInFuture(), program.selectIncidentDatesInFuture()),
                        Timber::e)
        );

        //region SECTIONS
        Flowable<List<FieldViewModel>> valuesFlowable = formRepository.fieldValues();
        Flowable<List<FormSectionViewModel>> sectionsFlowable = formRepository.sections();
        Flowable<Result<RuleEffect>> ruleEffectFlowable = ruleEngineRepository.calculate()
                .subscribeOn(schedulerProvider.computation());

        // Combining results of two repositories into a single stream.
        Flowable<List<FormSectionViewModel>> sectionModelsFlowable = Flowable.zip(
                sectionsFlowable, ruleEffectFlowable, this::applyEffects);

        compositeDisposable.add(processor.startWith("init")
                .flatMap(data -> sectionModelsFlowable)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(view.renderSectionViewModels(), Timber::e));


        /*private List<FormSectionViewModel> applyEffects(@NonNull List<FormSectionViewModel> viewModels,
                @NonNull Result<RuleEffect> calcResult) {
            if (calcResult.error() != null) {
                calcResult.error().printStackTrace();
                return viewModels;
            }

            Map<String, FieldViewModel> fieldViewModels = toMap(viewModels);
            applyRuleEffects(fieldViewModels, calcResult);

            return new ArrayList<>(fieldViewModels.values());
        }*/

        //endregion

        compositeDisposable.add(view.reportDateChanged()
                .subscribeOn(schedulerProvider.ui())
                .observeOn(schedulerProvider.io())
                .subscribe(formRepository.storeReportDate(), Timber::e));

        compositeDisposable.add(view.incidentDateChanged()
                .filter(date -> date != null)
                .subscribeOn(schedulerProvider.ui())
                .observeOn(schedulerProvider.io())
                .subscribe(formRepository.storeIncidentDate(), Timber::e));

        compositeDisposable.add(view.reportCoordinatesChanged()
                .filter(latLng -> latLng != null)
                .subscribeOn(schedulerProvider.ui())
                .observeOn(schedulerProvider.io())
                .subscribe(formRepository.storeCoordinates(), Timber::e));

        /*ConnectableFlowable<ReportStatus> statusObservable = formRepository.reportStatus()
                .distinctUntilChanged()
                .publish();*/

       /* compositeDisposable.add(statusObservable
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .skip(1)
                .subscribe(view::renderStatusChangeSnackBar, throwable -> {
                    throw new OnErrorNotImplementedException(throwable);
                }));

        compositeDisposable.add(statusObservable
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(view.renderStatus(), throwable -> {
                    throw new OnErrorNotImplementedException(throwable);
                }));

        compositeDisposable.add(statusObservable.connect());*/

        ConnectableObservable<ReportStatus> statusChangeObservable = view.eventStatusChanged()
//                .distinctUntilChanged()
                .publish();

        compositeDisposable.add(statusChangeObservable
                .filter(eventStatus -> formViewArguments.type() != FormViewArguments.Type.ENROLLMENT)
                .subscribeOn(schedulerProvider.ui())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(/*formRepository.storeReportStatus()*/view::onNext, throwable -> {
                    throw new OnErrorNotImplementedException(throwable);
                }));

        Observable<String> enrollmentDoneStream = statusChangeObservable
                .filter(eventStatus -> formViewArguments.type() == FormViewArguments.Type.ENROLLMENT)
                .map(reportStatus -> formViewArguments.uid())
                .observeOn(schedulerProvider.io()).share();

     /*   compositeDisposable.add(enrollmentDoneStream
                .subscribeOn(schedulerProvider.io())
                .subscribe(
                        formRepository.autoGenerateEvent(),
                        throwable -> {
                            throw new OnErrorNotImplementedException(throwable);
                        }));*/

        compositeDisposable.add(enrollmentDoneStream
                .flatMap(formRepository::autoGenerateEvents) //Autogeneration of events
                .flatMap(data -> formRepository.useFirstStageDuringRegistration()) //Checks if first Stage Should be used
                .subscribeOn(schedulerProvider.io())
                .subscribe(view.finishEnrollment(), throwable -> {
                    throw new OnErrorNotImplementedException(throwable);
                }));

        compositeDisposable.add(statusChangeObservable.connect());
    }

    @NonNull
    private List<FormSectionViewModel> applyEffects(
            @NonNull List<FormSectionViewModel> viewModels,
            @NonNull Result<RuleEffect> calcResult) {
        if (calcResult.error() != null) {
            calcResult.error().printStackTrace();
            return viewModels;
        }

        Map<String, FormSectionViewModel> fieldViewModels = toMap(viewModels);
        applyRuleEffects(fieldViewModels, calcResult);

        return new ArrayList<>(fieldViewModels.values());
    }

    private void applyRuleEffects(Map<String, FormSectionViewModel> fieldViewModels, Result<RuleEffect> calcResult) {
        //TODO: APPLY RULE EFFECTS TO ALL MODELS
        for (RuleEffect ruleEffect : calcResult.items()) {
            RuleAction ruleAction = ruleEffect.ruleAction();

            if (ruleAction instanceof RuleActionHideSection) {
                RuleActionHideSection hideSection = (RuleActionHideSection) ruleAction;
                fieldViewModels.remove(hideSection.programStageSection());
            }
        }
    }

    @NonNull
    private static Map<String, FormSectionViewModel> toMap(@NonNull List<FormSectionViewModel> fieldViewModels) {
        Map<String, FormSectionViewModel> map = new LinkedHashMap<>();
        for (FormSectionViewModel fieldViewModel : fieldViewModels) {
            map.put(fieldViewModel.sectionUid(), fieldViewModel);
        }
        return map;
    }

    @Override
    public void onDetach() {
        compositeDisposable.clear();
    }

    @Override
    public void checkSections() {
        processor.onNext("check");
    }


}