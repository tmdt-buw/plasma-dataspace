import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatSort } from '@angular/material/sort';
import { MatTable, MatTableDataSource } from '@angular/material/table';
import { DataSourceControllerService, DataSourceDTO } from '../../api/generated';
import { Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { PlsDataSourcesDescriptionComponent } from '../data-sources-desciption/data-sources-description.component';
import { Router } from '@angular/router';


@Component({
  selector: 'pls-data-sources',
  templateUrl: './data-sources-list.component.html',
  styleUrls: ['./data-sources-list.component.scss']
})
export class PlsDataSourcesComponent implements OnInit {

  // navigation routes
  @Input() dataSourcesRoute: string;
  @Input() schemaAnalysisRoute: string;
  @Input() modelingRoute: string;
  @Input() fwdNavigation: string = 'fwd';
  // extern var to trigger reload
  @Input() $refresh: Subject<any>;
  // table vars
  displayedColumns: string[] = ['title', 'description', 'operations'];
  dataSources = new MatTableDataSource<DataSourceDTO>([]);
  isLoading = false;
  error = null;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<DataSourceDTO>;
  // output to emit created event
  @Output() dataSourceSelected = new EventEmitter<DataSourceDTO>();

  constructor(private dataSourceController: DataSourceControllerService, private router: Router, public dialog: MatDialog) {
  }

  /**
   * Perform actions to init view
   */
  ngOnInit() {
    this.dataSources.sort = this.sort;
    this.getDataSources();
    this.$refresh.subscribe(() => {
      this.getDataSources();
    });
  }

  /**
   * Get data sources from backend
   */
  getDataSources(): void {
    this.isLoading = true;
    this.error = null;
    this.dataSourceController.getAllDataSources().subscribe(
      (dataSources) => {
        this.dataSources.data = dataSources;
        this.isLoading = false;
      }, error => {
        this.error = error;
        this.isLoading = false;
      });
  }

  /**
   * Opens dialog to view long description
   * @param dataSource input
   */
  showDataSourceLongDescription(dataSource: DataSourceDTO): void {
    this.dialog.open(PlsDataSourcesDescriptionComponent, {
      data: {
        title: dataSource.title,
        text: dataSource.longDescription
      },
    });
  }

  /**
   * Opens editor view for given route
   * @param dataSource to navigate to
   * @param route navigation
   */
  navigateToEditor(dataSource: DataSourceDTO, route: string): void {
    const extras = this.dataSourcesRoute ? {
      queryParams: {
        [this.fwdNavigation]: this.dataSourcesRoute
      }
    } : undefined;
    this.router.navigate([`${route}/${dataSource.uuid}`], extras);
  }

  /**
   * Delete a data source
   * @param dataSource to delete
   */
  deleteDataSource(dataSource: DataSourceDTO): void {
    this.isLoading = true;
    this.error = null;
    this.dataSourceController.deleteDataSource(dataSource.uuid).subscribe(() => {
        const index = this.dataSources.data.indexOf(dataSource);
        this.dataSources.data.splice(index, 1);
        this.table.renderRows();
        this.isLoading = false;
      }, error => {
        this.error = error;
        this.isLoading = false;
      }
    );
  }
}
