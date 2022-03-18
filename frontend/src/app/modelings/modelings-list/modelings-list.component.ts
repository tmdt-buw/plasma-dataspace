import { Component, EventEmitter, OnInit, Output} from '@angular/core';
import { Router } from '@angular/router';
import { ModelingControllerService, ModelingInfo } from '@tmdt/dms';
import { DatePipe } from '@angular/common';
import { NzModalService } from 'ng-zorro-antd/modal';
import { appRoutesNames } from '../../app.routes.names';

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

  constructor(private modelingController: ModelingControllerService, private router: Router, private date: DatePipe,
              private modal: NzModalService) {
  }

  /**
   * Perform actions to init view
   */
  ngOnInit(): void {
    this.getModelings();
  }

  /**
   * Get available modelings from backend
   */
  getModelings(): void {
    this.isLoading = true;
    this.error = null;
    this.modelingController.listModelings().subscribe(
      (modelings) => {
        this.modelings = modelings;
        this.isLoading = false;
      }, error => {
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
}
