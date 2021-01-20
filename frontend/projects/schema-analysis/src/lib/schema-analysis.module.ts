import { ModuleWithProviders, NgModule } from '@angular/core';
import { PlsSchemaAnalysisComponent } from './schema-analysis/schema-analysis.component';
import { ApiModule, BASE_PATH } from './api/generated';
import { CommonModule } from '@angular/common';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PlsLoadingButtonModule } from '@data-platform/common';
import { JsonViewerModule } from './json-viewer/json-viewer.module';



@NgModule({
  declarations: [PlsSchemaAnalysisComponent],
  imports: [
    // Plasma
    ApiModule,
    PlsLoadingButtonModule,
    // Angular
    CommonModule,
    FlexLayoutModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    FormsModule,
    ReactiveFormsModule,
    JsonViewerModule
  ],
  exports: [PlsSchemaAnalysisComponent]
})
export class PlsSchemaAnalysisModule {
  public static setBasePath(basePath: string): ModuleWithProviders<PlsSchemaAnalysisModule> {
    return {
      ngModule: PlsSchemaAnalysisModule,
      providers: [{provide: BASE_PATH, useValue: basePath}]
    };
  }
}
