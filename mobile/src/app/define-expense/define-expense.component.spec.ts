import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { DefineExpenseComponent } from './define-expense.component';

describe('DefineExpenseComponent', () => {
  let component: DefineExpenseComponent;
  let fixture: ComponentFixture<DefineExpenseComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
    imports: [IonicModule.forRoot(), DefineExpenseComponent]
}).compileComponents();

    fixture = TestBed.createComponent(DefineExpenseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
