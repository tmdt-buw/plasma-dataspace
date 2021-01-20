import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { PlsDataSourceDialogComponent } from './data-source-dialog/data-source-dialog.component';
import { PlsDataSourceComponent } from './data-source/data-source.component';
import { BytesPipe } from './data-source/upload/upload-card/bytes.pipe';
import { PlsUploadCardComponent } from './data-source/upload/upload-card/upload-card.component';
import { MatDialogModule } from '@angular/material/dialog';
import { PlsUploadDialogService } from './data-source-dialog/data-source-dialog.service';
import { FileDropModule } from '@data-platform/common';


@NgModule({
  declarations: [BytesPipe, PlsDataSourceComponent, PlsUploadCardComponent, PlsDataSourceDialogComponent],
  imports: [
    // Angular
    CommonModule,
    FileDropModule,
    FlexLayoutModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSortModule,
    MatTableModule,
    ReactiveFormsModule
  ],
  exports: [PlsDataSourceComponent, PlsDataSourceDialogComponent],
  providers: [PlsUploadDialogService]
})
export class PlsDataSourceModule {
}
