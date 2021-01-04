import { Component, ElementRef, ViewChild } from '@angular/core';
import { UniversalKnowledgeGraphControllerService } from '../../../api/generated/dms/api/kgsController.service';

@Component({
  selector: 'pls-import-dialog',
  templateUrl: './import-dialog.component.html',
  styleUrls: ['../common/dialog.styles.scss', 'import-dialog.component.scss']
})
export class PlsImportDialogComponent {

  @ViewChild('fileInput') fileInput: ElementRef;

  constructor(private kgsService: UniversalKnowledgeGraphControllerService) {
  }

  /**
   * function handles drag and drop of file for upload
   * @event - catches the dropped file
   */
  dropped(files: File | File[]) {
    if (Array.isArray(files)) {
      // files.forEach(file => this.handleFile(file));
      console.error('Multiple files not supported');
    } else {
      const reader = new FileReader();
      reader.readAsText(files, 'UTF-8');
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          this.kgsService.importRdf(reader.result).subscribe();
        }
      }
    }
  }

  /**
   * function for select file for upload
   */
  select() {
    const files = this.fileInput.nativeElement.files;
    for (let i = 0; i < files.length; i++) {
      const reader = new FileReader();
      reader.readAsText(files[i], 'UTF-8');
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          this.kgsService.importRdf(reader.result).subscribe();
        }
      }
    }
  }

}
