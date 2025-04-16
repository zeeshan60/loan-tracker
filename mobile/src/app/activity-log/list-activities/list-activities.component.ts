import { Transaction } from '../../friends/model';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import {
  IonAvatar, IonBackButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon, IonItem, IonLabel, IonList, IonNav,
  IonTitle,
  IonToolbar, LoadingController,
} from '@ionic/angular/standalone';
import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { ShortenNamePipe } from '../../pipes/shorten-name.pipe';
import { HttpClient } from '@angular/common/http';
import { PRIVATE_API } from '../../constants';
import {
  BehaviorSubject,
  catchError,
  filter,
  finalize,
  map,
  Observable,
  of, ReplaySubject,
  Subject,
  switchMap, takeUntil,
  tap,
  throwError,
} from 'rxjs';
import { TransactionDetailsComponent } from '../../friends/transaction-details/transaction-details.component';
import { HelperService } from '../../helper.service';
import { FriendsStore } from '../../friends/friends.store';
import { ComponentDestroyedMixin } from '../../component-destroyed.mixin';

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
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonIcon, IonButtons, CurrencyPipe, IonAvatar, IonItem, IonLabel, IonList, AsyncPipe, ShortenNamePipe, DatePipe, IonBackButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListActivitiesComponent extends ComponentDestroyedMixin() implements OnInit {
  readonly nav = inject(IonNav)
  readonly loadingCtrl = inject(LoadingController);
  protected readonly ActivityType = ActivityType;
  protected readonly ActivityTypeLabel = ActivityTypeLabel;
  readonly http = inject(HttpClient);
  readonly helperService = inject(HelperService);
  readonly refreshActivities$ = input.required<ReplaySubject<boolean>>();
  activities$: BehaviorSubject<Activity[]> = new BehaviorSubject([] as Activity[]);
  constructor() {
    super()
  }

  async ngOnInit() {
    let loader = await this.loadingCtrl.create();
    this.refreshActivities$()
      .pipe(
        tap(async (value) => {
          await loader.present();
        }),
        switchMap(() => this.http
          .get<{ data: Activity[]}>(`${PRIVATE_API}/transactions/activityLogs`)
        ),
        map(response => response.data),
        finalize(async () => {
          await loader.dismiss()
        }),
        catchError(() => {
          this.helperService.showToast('Unable to fetch activities at the moment.');
          return throwError(() => new Error());
        }),
        takeUntil(this.componentDestroyed)
      )
      .subscribe((activities) => {
        loader.dismiss()
        this.activities$.next(activities);
      });
  }

  openTransaction(activity: any) {
    this.nav.push(TransactionDetailsComponent, {
      transaction: activity.transactionResponse,
      friend: activity.transactionResponse.friend
    })
  }

  ionViewWillEnter() {
    this.refreshActivities$().next(true);
  }

}
