import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { ModelingControllerService, ModelingInfo, PlsModelingComponent } from '@tmdt/dms';
import { DatePipe } from '@angular/common';
import { ModalOptions, NzModalService } from 'ng-zorro-antd/modal';
import { appRoutesNames } from '../../app.routes.names';
import { NzNotificationService } from 'ng-zorro-antd/notification';
import { HttpStatusCode } from '@angular/common/http';
import { DataProcessingControllerService } from '../../dataprocessing/api/generated';
import { ModalBaseConfig } from '../../../../projects/modeling/src/lib/modeling/dialogs/common/modal.base-config';
import { ConversionDialogComponent } from '../../dataprocessing/conversion-dialog/conversion-dialog.component';
import { OntologyControllerService } from '../../../../projects/modeling/src/lib/api/generated/kgs';
import { Util } from 'projects/modeling/src/lib/modeling/model/common/util';

@Component({
  selector: 'app-modelings-list',
  templateUrl: './modelings-list.component.html',
  styleUrls: ['./modelings-list.component.less'],
  providers: [DatePipe]
})
export class ModelingsListComponent implements OnInit {

  modelings: ModelingInfo[] = [];
  isLoading = false;
  error = null;
  modelsRoute: string = appRoutesNames.MODELS;

  // output to emit created event
  @Output() modelingSelected = new EventEmitter<ModelingInfo>();
  dpsAvailable: boolean;

  constructor(private modelingController: ModelingControllerService, private router: Router, private date: DatePipe,
              private modal: NzModalService, private notification: NzNotificationService, private processingService: DataProcessingControllerService, private ontologyService: OntologyControllerService) {
  }

  /**
   * Perform actions to init view
   */
  ngOnInit(): void {
    this.getModelings();
    this.processingService.isAvailable().subscribe(() => this.dpsAvailable = true,
      () => this.dpsAvailable = false);
  }

  /**
   * Get available modelings from backend
   */
  getModelings(): void {
    this.isLoading = true;
    this.error = null;
    this.modelingController.listModelings().subscribe(
      (modelings) => {
        this.error = null;
        this.modelings = modelings;
        this.isLoading = false;
      }, error => {
        if (error.status === 0) {
          this.notification.error('Connection error occurred', 'This is most likely caused by a CORS error. Check the system setup.');
        } else if (error.status === HttpStatusCode.ServiceUnavailable) {
          this.notification.error('Modeling Service Not Available', 'The modeling service could not be reached. Please try again in a few minutes or contact support.');
        } else {
          this.notification.error('Unexpected error occurred', 'The modeling service returned:<br>' + error.statusText + ': ' + error.message);
        }
        this.error = error;
        this.isLoading = false;
      });
  }


  /**
   * Delete a modeling
   * @param modeling to delete
   */
  deleteModeling(modeling: ModelingInfo): void {
    this.isLoading = true;
    this.error = null;
    this.modelingController.deleteModel(modeling.id).subscribe(() => {
        const index = this.modelings.indexOf(modeling);
        this.modelings.splice(index, 1);
        this.isLoading = false;
      }, error => {
        this.error = error;
        this.isLoading = false;
      }
    );
  }

  showDescriptionModal(title: string, description: string): void {
    this.modal.create({
      nzTitle: title,
      nzContent: description,
      nzWidth: '70%',
      nzClosable: false,
      nzCancelText: null,
      nzOkText: 'Close'
    });
  }

  openConversionModal(model: ModelingInfo): void {
    const config: ModalOptions = {
      nzTitle: 'Convert ' + model.name,
      nzStyle: {position: 'absolute', top: `20%`, left: `25%`, width: '50%', bottom: '20%'},
      nzContent: ConversionDialogComponent,
      nzComponentParams: {
        model
      }
    };
    const ref = this.modal.create(Object.assign(config, ModalBaseConfig));
  }


  downloadLocalOntology(): void {
    this.ontologyService.downloadLocalOntology('TURTLE').subscribe(res => {
        Util.download('local_ontology.ttl', res);
      }
      , err => this.notification.error('Download failed', 'Cannot download the local ontology: ' + err.message));
  }
}
