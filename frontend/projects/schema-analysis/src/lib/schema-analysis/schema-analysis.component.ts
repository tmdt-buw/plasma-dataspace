import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { AnalysisControllerService, SchemaAnalysisDataProvisionDTO } from '../api/generated';
import { Subscription } from 'rxjs';

@Component({
  selector: 'pls-schema-analysis',
  templateUrl: './schema-analysis.component.html',
  styleUrls: ['./schema-analysis.component.scss']
})
export class PlsSchemaAnalysisComponent implements OnInit, OnDestroy {

  @Input() dataSource: string;
  initializing: boolean = true;
  lastDataPoints: string[] = [];

  currentDataPoint = '{"lat": 34, "lon": 234}';
  result: any;
  isReady: boolean;
  initSubscription: Subscription;

  loading;
  error;

  constructor(private schemaService: AnalysisControllerService) {
  }

  ngOnInit(): void {
    this.schemaService.existing(this.dataSource).subscribe(initialized => {
      if (!initialized) {
        this.schemaService.initAnalysis(this.dataSource).subscribe(() => this.initializing = false);
      } else {
        this.initializing = false;
        this.getIsReady();
      }
    }, error => this.error = error.error.message);
  }

  ngOnDestroy(): void {
    if (this.initSubscription) {
      this.initSubscription.unsubscribe();
    }
  }

  getIsReady(): void {
    this.loading = true;
    this.schemaService.isReady(this.dataSource).subscribe((ready) => {
      this.isReady = ready;
      if (ready) {
        this.getResult();
      } else {
        this.loading = false;
      }
    });
  }

  addLineSample(): void {
    const data = JSON.parse(this.currentDataPoint);

    if (Array.isArray(data)) {
      console.log('is array');
      const sendDataDto = [] ;
      data.forEach( sample => {
        sendDataDto.push({data: JSON.stringify(sample)});
      });

      this.schemaService.addDataPoints(this.dataSource, sendDataDto).subscribe( () => {
        sendDataDto.forEach(x => {
          this.lastDataPoints.push(x.data);
        });
      });

    } else {
      this.addSample(data);
    }
  }



  addSample(sample: any): void {
    const sampleString = JSON.stringify(sample);
    const sendDataDto: SchemaAnalysisDataProvisionDTO = {data: sampleString};
    this.schemaService.addDataPoint(this.dataSource, sendDataDto).subscribe(() => {
      this.lastDataPoints.push(sampleString);
    });
  }

  finish(): void {
    if (!this.isReady) {
      this.loading = true;
      this.schemaService.finish(this.dataSource).subscribe(() => this.getIsReady());
    }
  }

  getResult(): void {
    this.schemaService.getResult(this.dataSource).subscribe((result) => {
      this.result = result;
      this.loading = false;
    });
  }

  isValidJson(): boolean {
    try {
      JSON.parse(this.currentDataPoint);
      return true;
    } catch (e) {
      return false;
    }
  }

}
