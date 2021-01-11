import { NgModule } from '@angular/core';
import { PlsOperationsDialogComponent } from './operations-dialog/operations-dialog.component';
import { PlsSchemaFormModule } from './schema-form/schema-form.module';
import { CommonModule } from '@angular/common';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';

@NgModule({
  declarations: [PlsOperationsDialogComponent],
  imports: [
    // Plasma
    PlsSchemaFormModule,
    // Angular
    CommonModule,
    FlexLayoutModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatFormFieldModule,
  ],
  exports: [PlsOperationsDialogComponent]
})
export class PlsOperationsDialogModule {
}
