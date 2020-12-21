import { Component, Input, OnInit } from '@angular/core';
import { AnalysisControllerService, NodeDTO, SchemaAnalysisDataProvisionDTO, StandardDTO } from '../api/generated';
import { Subscription } from 'rxjs';

@Component({
  selector: 'pls-schema-analysis',
  templateUrl: './schema-analysis.component.html',
  styleUrls: ['./schema-analysis.component.scss']
})
export class PlsSchemaAnalysisComponent implements OnInit {

  @Input() dataSource: string;
  initializing: boolean = true;

  standards: StandardDTO[];
  usedStandard: StandardDTO;

  lastDataPoints: string[] = [];

  currentDataPoint = '{"lat": 34, "lon": 234}';
  // tslint:disable-next-line:max-line-length
  fullGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinate":{"longitude":7.2746115909,"latitude":51.2778283936}},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"MultiPoint","points":[{"type":"Point","coordinate":{"longitude":7.2746115909,"latitude":51.2778283936}},{"type":"Point","coordinate":{"longitude":7.2747184496,"latitude":51.2778641239}}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"MultiLineString","lines":[{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]},{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]}]},"properties":{"key1":"value1","key2":"value2"}},{"type":"Feature","geometry":{"type":"MultiPolygon","polygons":[{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]},{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]}]},"properties":{"key1":"value1","key2":"value2"}}]}';
  // tslint:disable-next-line:max-line-length
  pointGeo = '{\n' +
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
  lineGeo = '{\n' +
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
  polygonGeo = '{\n' +
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
  multiPointGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"MultiPoint","points":[{"type":"Point","coordinate":{"longitude":7.2746115909,"latitude":51.2778283936}},{"type":"Point","coordinate":{"longitude":7.2747184496,"latitude":51.2778641239}}]},"properties":{"key1":"value1","key2":"value2"}}]}';
  // tslint:disable-next-line:max-line-length
  multiLineGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"MultiLineString","lines":[{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]},{"type":"LineString","line":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239}]}]},"properties":{"key1":"value1","key2":"value2"}}]}';
  // tslint:disable-next-line:max-line-length
  multiPolygonGeo = '{"name":"TestCollection","type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"MultiPolygon","polygons":[{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]},{"type":"Polygon","outerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}],"innerPolygon":[{"longitude":7.2746115909,"latitude":51.2778283936},{"longitude":7.2747184496,"latitude":51.2778641239},{"longitude":7.2746115909,"latitude":51.2778283936}]}]},"properties":{"key1":"value1","key2":"value2"}}]}';

  schemaAnalysisResult: string;
  schemaAnalysisReady: boolean;
  initSubscription: Subscription;

  constructor(private schemaService: AnalysisControllerService) { }

  ngOnInit() {
    this.schemaService.existing(this.dataSource).subscribe(initialized => {
      if (!initialized) {
        this.schemaService.initAnalysis(this.dataSource).subscribe(() => this.initializing = false);
      } else {
        this.initializing = false;
      }
    });
  }

  ngOnDestroy() {
    if (this.initSubscription) {
      this.initSubscription.unsubscribe();
    }
  }

  isReady() {
    this.schemaService.isReady(this.dataSource).subscribe((ready) => this.schemaAnalysisReady = ready);
  }

  addLineSample() {
    let data = JSON.parse(this.currentDataPoint);
    if (Array.isArray(data)) {
      data.forEach(point => this.addSample(point));
    } else {
      this.addSample(data);
    }
  }

  addSample(sample: any) {
    const sampleString = JSON.stringify(sample);
    const sendDataDto: SchemaAnalysisDataProvisionDTO = {data: sampleString};
    this.schemaService.addDataPoint(this.dataSource, sendDataDto).subscribe();
    this.lastDataPoints.push(sampleString);
  }

  finish() {
    this.schemaService.finish(this.dataSource).subscribe(() => this.isReady());
  }

  getResult() {
    this.schemaService.getResult(this.dataSource).subscribe((result) => {
      this.schemaAnalysisResult = JSON.stringify(result, undefined, 4)
    });
  }

  isValidJson() {
    try {
      JSON.parse(this.currentDataPoint);
      return true;
    } catch (e) {
      return false;
    }
  }

  setStandard() {
    this.schemaService.assignStandard(this.dataSource, this.usedStandard.uuid).subscribe();
  }

}
