import { NgModule } from '@angular/core';
import * as sas from '@tmdt/sas';
import { environment } from '../../environments/environment';
import { CommonModule } from '@angular/common';
import { FlexLayoutModule } from '@angular/flex-layout';
import { EditorModule } from '../shared/editor/editor.module';
import { RouterModule } from '@angular/router';
import { SchemaAnalysisComponent } from './schema-analysis/schema-analysis.component';
import { SAS_ROUTES } from './schema-analysis.routes';

@NgModule({
  declarations: [SchemaAnalysisComponent],
  imports: [
    // Plasma
    sas.PlsSchemaAnalysisModule.setBasePath(environment.baseUrl),
    // Angular
    CommonModule,
    FlexLayoutModule,
    EditorModule,
    RouterModule.forChild(SAS_ROUTES),
  ]
})
export class SchemaAnalysisModule {
}
