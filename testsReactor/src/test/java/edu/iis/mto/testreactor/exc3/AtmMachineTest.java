package edu.iis.mto.testreactor.exc3;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

public class AtmMachineTest {

    private CardProviderService cardProviderService;
    private BankService bankService;
    private MoneyDepot moneyDepot;
    private AtmMachine atmMachine;
    private Card card;
    private AuthenticationToken authenticationToken;

    @Before
    public void init() {
        cardProviderService = Mockito.mock(CardProviderService.class);
        bankService = Mockito.mock(BankService.class);
        moneyDepot = Mockito.mock(MoneyDepot.class);

        atmMachine = new AtmMachine(cardProviderService, bankService, moneyDepot);
        card = Card.builder()
                .withCardNumber("test")
                .withPinNumber(1111)
                .build();
        authenticationToken = AuthenticationToken.builder()
                .withAuthorizationCode(1111)
                .withUserId("1")
                .build();
    }

    @Test
    public void itCompiles() {
        assertThat(true, equalTo(true));
    }

    @Test(expected = CardAuthorizationException.class)
    public void shouldThrowCardAuthorizationExceptionWhenAuthCodeNotPresent() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.ofNullable(null));

        atmMachine.withdraw(money, card);
    }

    @Test(expected = WrongMoneyAmountException.class)
    public void shouldThrowWrongMoneyAmountExceptionWhenMoneyAmountIsLessOrEqualZero() {
        Money money = Money.builder()
                .withAmount(0)
                .withCurrency(Currency.PL)
                .build();

        atmMachine.withdraw(money, card);
    }

    @Test(expected = WrongMoneyAmountException.class)
    public void shouldThrowWrongMoneyAmountExceptionWhenMoneyAmountCannotBePayedWithBanknotes() {
        Money money = Money.builder()
                .withAmount(3)
                .withCurrency(Currency.PL)
                .build();

        atmMachine.withdraw(money, card);
    }

    @Test(expected = InsufficientFundsException.class)
    public void shouldThrowInsufficientFundsExceptionWhenChargeReturnsFalse() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(false);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        atmMachine.withdraw(money, card);
    }

    @Test(expected = MoneyDepotException.class)
    public void shouldThrowMoneyDepotExceptionWhenMoneyDepotFailToReleaseMoney() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(false);

        atmMachine.withdraw(money, card);
    }

    @Test
    public void shouldReturnPaymentWithCorrectMoneyAmount() {
        Money money = Money.builder()
                .withAmount(10)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(true);

        Payment payment = atmMachine.withdraw(money, card);
        int sum = 0;
        for (Banknote banknote : payment.getValue()) {
            sum += banknote.getValue();
        }

        Assert.assertEquals(10, sum);
    }

    @Test
    public void shouldReturnPaymentWithCorrectAmountOfBanknotes() {
        Money money = Money.builder()
                .withAmount(350)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(true);

        Payment payment = atmMachine.withdraw(money, card);

        Assert.assertEquals(3, payment.getValue()
                .size());
    }

    @Test
    public void shouldBankServiceCallStartTransactionAndCommitOnceWhenWithdrawalIsSuccessful() {
        Money money = Money.builder()
                .withAmount(250)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(true);

        Payment payment = atmMachine.withdraw(money, card);

        Mockito.verify(bankService, Mockito.times(1))
                .startTransaction(Mockito.any(AuthenticationToken.class));
        Mockito.verify(bankService, Mockito.times(1))
                .commit(Mockito.any(AuthenticationToken.class));
    }

    @Test
    public void shouldReturnPaymentWithCorrectCurrencyOfBanknotes() {
        Money money = Money.builder()
                .withAmount(250)
                .withCurrency(Currency.EU)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(true);

        Payment payment = atmMachine.withdraw(money, card);

        for (Banknote banknote : payment.getValue()) {
            Assert.assertEquals(Currency.EU, banknote.getCurrency());
        }
    }

    @Test
    public void shouldBankServiceCallAbortOnceWhenMoneyDepotFailToReleaseMoney() {
        Money money = Money.builder()
                .withAmount(250)
                .withCurrency(Currency.EU)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(false);

        try {
            atmMachine.withdraw(money, card);
            fail();
        } catch (Exception ex) {
            Mockito.verify(bankService, Mockito.times(1))
                    .abort(Mockito.any(AuthenticationToken.class));
        }
    }

    @Test
    public void shouldReturnPaymentWithCorrectBanknotesValuesInDescendingOrder() {
        Money money = Money.builder()
                .withAmount(380)
                .withCurrency(Currency.PL)
                .build();

        Mockito.when(bankService.charge(Mockito.any(AuthenticationToken.class), Mockito.any(Money.class)))
                .thenReturn(true);

        Mockito.when(cardProviderService.authorize(Mockito.any(Card.class)))
                .thenReturn(Optional.of(authenticationToken));

        Mockito.when(moneyDepot.releaseBanknotes(Mockito.anyListOf(Banknote.class)))
                .thenReturn(true);

        List<Banknote> banknotes = atmMachine.withdraw(money, card).getValue();

        Assert.assertEquals(200, banknotes.get(4).getValue());
        Assert.assertEquals(100, banknotes.get(3).getValue());
        Assert.assertEquals(50, banknotes.get(2).getValue());
        Assert.assertEquals(20, banknotes.get(1).getValue());
        Assert.assertEquals(10, banknotes.get(0).getValue());
    }
}
