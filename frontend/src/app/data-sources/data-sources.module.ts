import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import * as dss from 'data-sources';
import { environment } from '../../environments/environment';
import { DSS_ROUTES } from './data-sources.routes';
import { DataSourcesComponent } from './data-sources/data-sources.component';


@NgModule({
  declarations: [DataSourcesComponent],
  imports: [
    //Plasma
    dss.PlsDataSourcesModule.setBasePath(environment.baseUrl),
    //Angular
    CommonModule,
    FlexLayoutModule,
    MatButtonModule,
    MatIconModule,
    RouterModule.forChild(DSS_ROUTES),
  ]
})
export class DataSourcesModule {
}
