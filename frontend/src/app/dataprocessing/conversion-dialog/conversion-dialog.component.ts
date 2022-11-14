import { Component, Input, OnInit } from '@angular/core';
import { CombinedModel, ModelingControllerService, ModelingInfo } from '@tmdt/dms';
import { DataProcessingControllerService } from '../api/generated';
import { NzNotificationService } from 'ng-zorro-antd/notification';
import { Util } from '../../../../projects/modeling/src/lib/modeling/model/common/util';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-conversion-dialog',
  templateUrl: './conversion-dialog.component.html',
  styleUrls: ['./conversion-dialog.component.less']
})
export class ConversionDialogComponent implements OnInit {

  @Input() model: ModelingInfo;
  fileList: Array<string>;
  selectedFile: string;

  constructor(private processingService: DataProcessingControllerService, private notification: NzNotificationService,
              private modelingService: ModelingControllerService) {
  }

  ngOnInit(): void {
    this.getFileList(this.model.dataId);
  }

  getFileList(dataId: string): void {
    // get file list from DPS
    this.processingService.listFiles(dataId).subscribe(
      data => this.fileList = data,
      error => {
        console.log(error);
      });
  }

  selectFile(fileName: string): void {
    this.selectedFile = fileName;
  }

  isSelected(fileName: string): boolean {
    return this.selectedFile === fileName;
  }

  triggerConversion(): void {
    this.modelingService.getCombinedModel(this.model.id).subscribe(
      (cm: CombinedModel) => {
        this.processingService.convertFile(this.model.dataId, cm, this.selectedFile).subscribe(rdf => {
            const filename = this.selectedFile.replace('.json', '.ttl');
            Util.download(filename, rdf);
          },
          error => {
            if (error.message.includes('service unavailable')) {
              this.notification.error('Error during export', 'Processing service not reachable');
            } else {
              this.notification.error('Error during export', 'Could not convert file: ' + error.message);
            }

          });
      },
      error => this.notification.error('Error during export', 'Could not obtain semantic model: ' + error.message));
  }

  uploadFile = (item): Subscription => {
    return this.processingService.uploadFile(item.file, this.model.dataId)
      .subscribe(sample => {
        this.notification.success('File uploaded', 'File has been uploaded');
        this.fileList.push(sample.filename);
      }, error => this.notification.error('Error during file upload', error.status + ':' + error));
  };
}
