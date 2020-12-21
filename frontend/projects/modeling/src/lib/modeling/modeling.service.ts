import { Injectable } from '@angular/core';
import { DeltaRecommendationDTO, RelationConceptDTO } from '../api/generated/dms';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ModelingService {
  relationConcept = new BehaviorSubject<RelationConceptDTO>(null);
  recommendation = new BehaviorSubject<DeltaRecommendationDTO>(null);
}
