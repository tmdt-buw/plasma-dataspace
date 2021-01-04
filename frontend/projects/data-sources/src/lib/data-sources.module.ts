import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { ApiModule, BASE_PATH } from './api/generated';
import { PlsDataSourceModule } from './data-source/data-source.module';
import { PlsDataSourceComponent } from './data-source/data-source/data-source.component';
import { PlsDataSourcesListModule } from './data-sources-list/data-sources-list.module';
import { PlsDataSourcesComponent } from './data-sources-list/data-sources-list/data-sources-list.component';

@NgModule({
  imports: [
    // Plasma
    ApiModule,
    PlsDataSourceModule,
    PlsDataSourcesListModule,
    // Angular
    CommonModule,
    FlexLayoutModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatInputModule,
    MatSortModule,
    MatTableModule,
    ReactiveFormsModule
  ],
  exports: [PlsDataSourceComponent, PlsDataSourcesComponent]
})
export class PlsDataSourcesModule {
  public static setBasePath(basePath: string): ModuleWithProviders<PlsDataSourcesModule> {
    return {
      ngModule: PlsDataSourcesModule,
      providers: [{provide: BASE_PATH, useValue: basePath}]
    };
  }
}
