import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IonNav } from '@ionic/angular/standalone';
import { ListActivitiesComponent } from './list-activities/list-activities.component';
import { BehaviorSubject, ReplaySubject, Subject } from 'rxjs';

@Component({
  selector: 'mr-activity-log',
  templateUrl: 'activity-log.page.html',
  styleUrls: ['activity-log.page.scss'],
  standalone: true,
  imports: [
    IonNav,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityLogPage {
  readonly listActivitiesComponent = ListActivitiesComponent;
  rootParams = {
    refreshActivities$: new ReplaySubject(1)
  }
  constructor() {}
  ionViewWillEnter() {
    this.rootParams.refreshActivities$.next(true);
  }
}
