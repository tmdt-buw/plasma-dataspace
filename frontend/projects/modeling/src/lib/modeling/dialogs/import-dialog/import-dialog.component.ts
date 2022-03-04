import { Component, ElementRef, ViewChild } from '@angular/core';
import { NzUploadChangeParam } from 'ng-zorro-antd/upload';
import { OntologyControllerService } from '../../../api/generated/kgs';

@Component({
  selector: 'pls-import-dialog',
  templateUrl: './import-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss', 'import-dialog.component.scss']
})
export class PlsImportDialogComponent {

  loading = false;

  @ViewChild('fileInput') fileInput: ElementRef;

  constructor(private ontologyService: OntologyControllerService) {
  }

  /**
   * function handles drag and drop of file for upload
   * @event - catches the dropped file
   */
  handleChange({file, fileList}: NzUploadChangeParam): void {
    this.loading = true;
    const reader = new FileReader();
    reader.readAsText(file.originFileObj, 'UTF-8');
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        // this.ontologyService.importRdf(reader.result).subscribe();
      }
    };
  }

}
