import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FWD } from '../../common/constants/routing';


/**
 * Editor view component with layout and standard header
 */
@Component({
  selector: 'app-editor',
  templateUrl: './editor.component.html',
  styleUrls: ['./editor.component.scss']
})
export class EditorComponent implements OnInit {

  url: string;

  constructor(private router: Router, private route: ActivatedRoute) {
  }

  /**
   * Set forward url var
   */
  ngOnInit(): void {
    this.url = this.route.snapshot.queryParamMap.get(FWD) || '/';
  }

  /**
   * Redirect to forward url on close
   */
  close(): void {
    this.router.navigate([this.url]);
  }

}
