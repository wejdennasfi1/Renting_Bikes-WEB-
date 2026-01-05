package rental.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BankStore {

    private static final Map<String, Double> balancesEur = new ConcurrentHashMap<>();

    static {
        // basic scenario accounts (EUR balances)
        balancesEur.put("ACC_client1", 2000.0);
        balancesEur.put("ACC_wejden", 200.0);
        balancesEur.put("ACC_test", 500.0);
    }

    public static synchronized double getBalanceEur(String accountId) {
        return balancesEur.getOrDefault(accountId, 0.0);
    }

    public static synchronized void setBalanceEur(String accountId, double eur) {
        balancesEur.put(accountId, eur);
    }

    public static synchronized boolean payEur(String accountId, double amountEur) {
        if (accountId == null || accountId.trim().isEmpty()) return false;
        if (amountEur <= 0) return false;

        double bal = getBalanceEur(accountId);
        if (bal < amountEur) return false;

        balancesEur.put(accountId, bal - amountEur);
        return true;
    }
}
