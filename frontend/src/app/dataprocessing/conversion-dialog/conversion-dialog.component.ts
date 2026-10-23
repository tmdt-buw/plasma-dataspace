import { Component, Input, OnInit } from '@angular/core';
import { CombinedModel, ModelingControllerService, ModelingInfo } from '@tmdt/smui';
import { DataProcessingControllerService } from '../api/generated';
import { NzNotificationService } from 'ng-zorro-antd/notification';
import { Subscription } from 'rxjs';
import { Util } from '../../../../projects/smui/src/lib/modeling/model/common/util';

@Component({
  selector: 'app-conversion-dialog',
  templateUrl: './conversion-dialog.component.html',
  styleUrls: ['./conversion-dialog.component.less']
})
export class ConversionDialogComponent implements OnInit {

  @Input() model: ModelingInfo;
  fileList: Array<string> = [];
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
    if (!this.selectedFile) return;
    this.modelingService.getCombinedModel(this.model.id).subscribe(
      (cm: CombinedModel) => {
        this.processingService.convertFile(this.model.dataId, cm, this.selectedFile).subscribe(rdf => {
            const filename = this.selectedFile.replace(/\.[^.]+$/, '') + '.ttl';
            Util.download(filename, rdf);
          },
          async error => {
            let message = error?.error?.message || error?.message || 'Conversion failed';
            if (typeof error?.error?.text === 'function') {
              try {
                const text = await error.error.text();
                try { message = JSON.parse(text).message || text; } catch { message = text || message; }
              } catch { /* Keep the HTTP error message if the body cannot be read. */ }
            }
            this.notification.error('Error during conversion', message);

          });
      },
      error => this.notification.error('Error during conversion', 'Could not obtain semantic model: ' + error.error.message));
  }

  uploadFile = (item): Subscription => {
    return this.processingService.uploadFile(item.file, this.model.dataId)
      .subscribe(sample => {
        this.notification.success('File uploaded', 'File has been uploaded');
        this.fileList.push(sample.filename);
      }, error => this.notification.error('Error during file upload', error.status + ':' + error.error.message));
  }
}
