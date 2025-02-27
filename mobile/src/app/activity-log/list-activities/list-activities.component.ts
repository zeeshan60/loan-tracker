import { Transaction } from '../../friends/model';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import {
  IonAvatar, IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon, IonItem, IonLabel, IonList, IonNav,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { HttpClient } from '@angular/common/http';
import { PRIVATE_API } from '../../constants';
import { catchError, finalize, map, Observable, of, Subject, switchMap, tap, throwError } from 'rxjs';
import { TransactionDetailsComponent } from '../../friends/transaction-details/transaction-details.component';
import { HelperService } from '../../helper.service';
import { FriendsStore } from '../../friends/friends.store';

enum ActivityType {
  CREATED = 'CREATED',
  UPDATED = 'UPDATED',
  DELETED = 'DELETED',
}
interface Activity {
  userUid: string,
  activityByName: string,
  activityByPhoto: string,
  description: string,
  activityType: ActivityType,
  amount: number,
  currency: string,
  isOwed: true,
  date: string,
  transactionResponse: Transaction
}
const ActivityTypeLabel = {
  [ActivityType.CREATED]: 'Added',
  [ActivityType.UPDATED]: 'Updated',
  [ActivityType.DELETED]: 'Deleted',
}

@Component({
  selector: 'app-list-activities',
  templateUrl: './list-activities.component.html',
  styleUrls: ['./list-activities.component.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons, CurrencyPipe, IonAvatar, IonItem, IonLabel, IonList, AsyncPipe, ShortenNamePipe, DatePipe, IonNav, IonBackButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListActivitiesComponent implements OnInit {
  readonly nav = inject(IonNav);
  protected readonly ActivityType = ActivityType;
  protected readonly ActivityTypeLabel = ActivityTypeLabel;
  readonly http = inject(HttpClient);
  readonly helperService = inject(HelperService);
  readonly friendStore = inject(FriendsStore);
  readonly refreshActivities$ = input.required<Subject<boolean>>();
  activities$: Observable<Activity[]> = of([]);
  constructor() {}

  ngOnInit() {
    this.activities$ = this.refreshActivities$()
      .pipe(
        tap(() => {
          this.friendStore.setLoading(true);
        }),
        switchMap(() => this.http
          .get<{ data: Activity[]}>(`${PRIVATE_API}/transactions/activityLogs`)
        ),
        tap(() => {
          this.friendStore.setLoading(false);
        }),
        map(response => response.data),
        finalize(() => {
          this.friendStore.setLoading(false);
        }),
        catchError(() => {
          this.helperService.showToast('Unable to fetch activities at the moment.');
          return throwError(() => new Error());
        }),
      );
  }

  openTransaction(activity: any) {
    this.nav.push(TransactionDetailsComponent, {
      transaction: activity.transactionResponse,
      friend: { name: activity.transactionResponse.friendName}
    })
  }

  ionViewWillEnter() {
    this.refreshActivities$().next(true);
  }

}
