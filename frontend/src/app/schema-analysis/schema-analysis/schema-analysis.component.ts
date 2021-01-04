import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-schema-analysis',
  templateUrl: './schema-analysis.component.html',
  styleUrls: ['./schema-analysis.component.scss']
})
export class SchemaAnalysisComponent implements OnInit {

  dataSource: string;

  constructor(private route: ActivatedRoute) { }

  ngOnInit(): void {
    this.dataSource = this.route.snapshot.paramMap.get('id');
  }

}
