import {Component, OnDestroy, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {
  EntityRuleDTO,
  NodeDTO,
  RelationRuleDTO,
  RuleProvisionDTO,
  SchemaAnalysisDataProvisionDTO,
  SelectorDTO
} from '../../../../build/openapi';
import {DataSourceDTO} from '../../../../build/openapi';
import {AnalysisControllerService} from '../../../../build/openapi/api/analysisController.service';
import {Subscription} from 'rxjs';
import {DataSourceControllerService} from '../../../../build/openapi/api/dataSourceController.service';


@Component({
  selector: 'app-datasources',
  templateUrl: './schemaanalysis.component.html',
  styleUrls: ['./schemaanalysis.component.scss']
})
export class SchemaanalysisComponent implements OnInit, OnDestroy {

  constructor(private http: HttpClient, private readonly dataSourceController: DataSourceControllerService,
              private readonly schemaanalysisController: AnalysisControllerService) {
    this.dataSourceController.getAllDataSources().subscribe(
      (datasource) => {
        this.allDataSources = datasource;
        this.allDataSources.sort((a, b) => {
          return (a.title.toUpperCase() < b.title.toUpperCase()) ? -1 : (a.title.toUpperCase() > b.title.toUpperCase()) ? 1 : 0;

        });
      });
  }

  allDataSources: DataSourceDTO[] = [];

  selectedDataSource: DataSourceDTO;


  lastDataPoints: string[] = [];

  public dataSourceInitialized: boolean;
  public currentDataPoint = '';
  // tslint:disable-next-line:max-line-length
  public fullGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinate":{"longitude":7.2746115909,"latitude":51.2778283936}},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"MultiPoint","points":[{"type":"Point","coordinate":{"longitude":7.2746115909,"latitude":51.2778283936}},{"type":"Point","coordinate":{"longitude":7.2747184496,"latitude":51.2778641239}}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"MultiLineString","lines":[{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]},{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"MultiPolygon","polygons":[{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]},{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]}]},"properties":{"key1":"value1","key2":"value2"}}]}';
  // tslint:disable-next-line:max-line-length
  public pointGeo = '{\n' +
    '  "type" : "Feature",\n' +
    '  "geometry" : {\n' +
    '        "type" : "Point",\n' +
    '        "coordinate" : {\n' +
    '          "longitude" : 7.2746115909,\n' +
    '          "latitude" : 51.2778283936\n' +
    '        }\n' +
    '      },\n' +
    '  "properties" : {\n' +
    '    "key1" : "value1",\n' +
    '    "key2" : "value2"\n' +
    '  },\n' +
    '  "collectionName" : "TestCollection"\n' +
    '}';
  // tslint:disable-next-line:max-line-length
  public lineGeo = '{\n' +
    '  "type" : "Feature",\n' +
    '  "geometry" : {\n' +
    '    "type" : "LineString",\n' +
    '    "line" : [{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]\n' +
    '  },\n' +
    '  "properties" : {\n' +
    '    "key1" : "value1",\n' +
    '    "key2" : "value2"\n' +
    '  },\n' +
    '  "collectionName" : "TestCollection"\n' +
    '}\n';
  // tslint:disable-next-line:max-line-length
  public polygonGeo = '{\n' +
    '  "type" : "Feature",\n' +
    '  "geometry" : {\n' +
    '        "type" : "Polygon",\n' +
    '        "outerPolygon" : [\n' +
    '          {\n' +
    '            "longitude" : 7.2746115909,\n' +
    '            "latitude" : 51.2778283936\n' +
    '          },\n' +
    '          {\n' +
    '            "longitude" : 7.2747184496,\n' +
    '            "latitude" : 51.2778641239\n' +
    '          },\n' +
    '          {\n' +
    '            "longitude" : 7.2746115909,\n' +
    '            "latitude" : 51.2778283936\n' +
    '          }\n' +
    '        ]\n' +
    '      },\n' +
    '  "properties" : {\n' +
    '    "key1" : "value1",\n' +
    '    "key2" : "value2"\n' +
    '  },\n' +
    '  "collectionName" : "TestCollection"\n' +
    '}\n';
  // tslint:disable-next-line:max-line-length
  public multiPointGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"MultiPoint","points":[{"type":"Point","coordinate":{"longitude":7.2746115909,"latitude":51.2778283936}},{"type":"Point","coordinate":{"longitude":7.2747184496,"latitude":51.2778641239}}]},"properties":{"key1":"value1","key2":"value2"}}]}';
  // tslint:disable-next-line:max-line-length
  public multiLineGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"MultiLineString","lines":[{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]},{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]}]},"properties":{"key1":"value1","key2":"value2"}}]}';
  // tslint:disable-next-line:max-line-length
  public multiPolygonGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"MultiPolygon","polygons":[{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]},{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]}]},"properties":{"key1":"value1","key2":"value2"}}]}';

  public schemaAnalysisRawResult: string;
  public schemaAnalysisResult: NodeDTO;
  public schemaAnalysisReady: boolean;
  public initSubscription: Subscription;

  ngOnInit() {
  }

  ngOnDestroy() {
    if (this.initSubscription) {
      this.initSubscription.unsubscribe();
    }
  }

  public async isInitialized() {
    this.dataSourceInitialized = await this.schemaanalysisController.existing(this.selectedDataSource.uuid).toPromise();
  }

  public async isReady() {
    this.schemaAnalysisReady = await this.schemaanalysisController.isReady(this.selectedDataSource.uuid).toPromise();
  }


  public async initSchemaAnalysis() {
    this.initSubscription = this.schemaanalysisController.initAnalysis(this.selectedDataSource.uuid).subscribe(
      () => {this.dataSourceInitialized = true; }
    );
  }

  addLineSample() {
    const sendDataDto: SchemaAnalysisDataProvisionDTO = {data: this.currentDataPoint};
    this.schemaanalysisController.addDataPoint(this.selectedDataSource.uuid, sendDataDto).toPromise();
    this.lastDataPoints.push(this.currentDataPoint);
  }

  finish() {
    this.schemaanalysisController.finish(this.selectedDataSource.uuid)
      .subscribe(async () => {await this.isReady(); });
  }

  public async getResult() {
    this.schemaAnalysisResult = await this.schemaanalysisController
      .getResult(this.selectedDataSource.uuid).toPromise();
    this.schemaAnalysisRawResult = JSON.stringify(this.schemaAnalysisResult, undefined, 4);
  }

  public async changeDatasource() {
    this.lastDataPoints = [];
    await this.isInitialized();
    await this.isReady();
    this.currentDataPoint = '';
    this.schemaAnalysisResult = null;
    this.schemaAnalysisRawResult = '';
  }

  public isValidJson() {
    try {
      JSON.parse(this.currentDataPoint);
      return true;
    } catch (e) {
      return false;
    }
  }
}
