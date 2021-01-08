import {Component, ElementRef, ViewChild} from '@angular/core';
import {UniversalKnowledgeGraphControllerService} from '../../../api/generated/kgs';

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
  dropped(files: File | File[]): void {
    if (Array.isArray(files)) {
      // multiple files not supported atm
      // files.forEach(file => this.handleFile(file));
      console.error('Multiple files not supported');
    } else {
      const reader = new FileReader();
      reader.readAsText(files, 'UTF-8');
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          this.kgsService.importRdf(reader.result).subscribe();
        }
      };
    }
  }

  /**
   * function for select file for upload
   */
  select(): void {
    const files = this.fileInput.nativeElement.files;
    files.forEach(file => {
      const reader = new FileReader();
      reader.readAsText(file, 'UTF-8');
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          this.kgsService.importRdf(reader.result).subscribe();
        }
      };
    });
  }

}
