import { Component, EventEmitter, Output} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { CombinedModel, ModelingControllerService, ModelingInfo, SyntaxModel } from '@tmdt/dms';
import { v4 as uuidv4 } from 'uuid';
import { NzNotificationService } from 'ng-zorro-antd/notification';
import { Router} from '@angular/router';
import { appRoutesNames } from '../../app.routes.names';
import { DataProcessingControllerService } from '../../dataprocessing/api/generated';
import { NzUploadFile } from 'ng-zorro-antd/upload';
import { AnalysisControllerService } from '../../schema-analysis/api/generated';
import { graphlib } from 'dagre';
import json = graphlib.json;

@Component({
  selector: 'app-create-modeling',
  templateUrl: './create-modeling.component.html',
  styleUrls: ['./create-modeling.component.less'],
})
export class CreateModelingComponent {

  dataId: string;
  modelId: string;
  syntaxModel: SyntaxModel;
  dpsAvailable = false;
  usingFiles = false;

  sampleData = '';
  sampleCounter = 0;

  files: File[] = [];

  // form
  form = new FormGroup({
    title: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]),
    description: new FormControl('', [Validators.required, Validators.maxLength(4000)])
  });
  error;
  success;

  @Output() modelingCreated = new EventEmitter();


  constructor(private modelingController: ModelingControllerService,
              private dataprocessingController: DataProcessingControllerService,
              private schemaAnalysisController: AnalysisControllerService,
              private notification: NzNotificationService,
              private router: Router) {
    this.dataId = uuidv4();
    this.modelId = this.dataId;
    this.dataprocessingController.isAvailable().subscribe(() => this.dpsAvailable = true, () => console.log('DPS not available'));
  }


  onSubmit(): void {
    this.error = null;
    const combinedModel: CombinedModel = {id: this.modelId, syntaxModel: this.syntaxModel};
    this.modelingController.createNewModeling(combinedModel, null, false,
      this.form.get('title').value, this.form.get('description').value, this.dataId)
      .subscribe((res: ModelingInfo) => {
        if ('id' in res) {
          this.form.reset();
          this.router.navigate([`${appRoutesNames.MODELS}/${res.id}`]);
        }
      }, error => {
        console.log('create error', error);
        this.error = error;
      });
  }

  /**
   * Returns true if form is valid or dps was used and a schema is available
   */
  isValid(): boolean {
    return this.form.valid && !!this.syntaxModel;
  }

  onSchemaAvailable(schema: SyntaxModel): void {
    this.syntaxModel = schema;
  }

  onSchemaAnalysisError(errorMessage: string): void {
    this.syntaxModel = undefined;
  }

  onFileAdded(info: {file: NzUploadFile }): void {
    this.usingFiles = true;
    this.dataprocessingController.uploadFile(info.file.originFileObj, this.dataId)
      .subscribe((response ) => {
        this.schemaAnalysisController.initAnalysis(this.dataId).subscribe(() => {
          response.sample.forEach(sample => {
            if (this.sampleCounter < 10) {
              this.schemaAnalysisController.addDataPoint(this.dataId, {data: JSON.stringify(JSON.parse(sample))});
              this.sampleCounter++;
            }
          });
        });

      });
  }
}
