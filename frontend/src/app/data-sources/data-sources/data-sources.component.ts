import { Component } from '@angular/core';
import { Subject } from 'rxjs';
import { PlsUploadDialogService } from '@tmdt/dss';
import { appRoutesNames } from '../../app.routes.names';
import { FWD } from '../../shared/common/constants/routing';

@Component({
  selector: 'app-data-sources',
  templateUrl: './data-sources.component.html',
  styleUrls: ['./data-sources.component.scss']
})
export class DataSourcesComponent {

  readonly routes = appRoutesNames;
  readonly fwdNavigation = FWD;

  $refresh = new Subject();

  constructor(private sourceDialogService: PlsUploadDialogService) {
  }

  /**
   * Open data source dialog and refresh data source list if data source was created
   */
  addDataSource(): void {
    this.sourceDialogService.show().subscribe((dataSourceCreated: boolean) => {
      if (dataSourceCreated) {
        this.$refresh.next();
      }
    });
  }
}
