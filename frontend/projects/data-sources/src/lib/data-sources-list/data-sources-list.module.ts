import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatIconModule } from '@angular/material/icon';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PlsDataSourcesComponent } from './data-sources-list/data-sources-list.component';
import { MatButtonModule } from '@angular/material/button';
import { PlsDataSourcesDescriptionComponent } from './data-sources-desciption/data-sources-description.component';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';

@NgModule({
  declarations: [PlsDataSourcesComponent, PlsDataSourcesDescriptionComponent],
  imports: [
    CommonModule,
    FlexLayoutModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSortModule,
    MatTableModule,
    MatTooltipModule
  ],
  exports: [PlsDataSourcesComponent]
})
export class PlsDataSourcesListModule {
}
