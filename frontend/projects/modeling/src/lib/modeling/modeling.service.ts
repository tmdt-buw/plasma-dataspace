import {Injectable} from '@angular/core';
import {DeltaModification, RelationConcept} from '../api/generated/dms';
import {BehaviorSubject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ModelingService {
  relationConcept = new BehaviorSubject<RelationConcept>(null);
  recommendation = new BehaviorSubject<DeltaModification>(null);
}
