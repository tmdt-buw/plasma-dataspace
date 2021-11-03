import { NgModule } from '@angular/core';
import * as dms from '@tmdt/dms';
import { environment } from '../../environments/environment';
import { CommonModule } from '@angular/common';
import { FlexLayoutModule } from '@angular/flex-layout';
import { RouterModule } from '@angular/router';
import { DMS_ROUTES } from './modeling.routes';
import { EditorModule } from '../shared/editor/editor.module';
import { ModelingComponent } from './modeling/modeling.component';


@NgModule({
  declarations: [ModelingComponent],
  imports: [
    // Plasma
    dms.PlsModelingModule.setBasePath(environment.baseUrl),
    // Angular
    CommonModule,
    FlexLayoutModule,
    EditorModule,
    RouterModule.forChild(DMS_ROUTES),
  ]
})
export class ModelingModule {
}
