package producttransfer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import com.owlike.genson.Genson;

import ilog.concert.*;
import ilog.cplex.*;

@Contract(name = "ProductTransfer", info = @Info(title = "ProductTransfer contract", description = "A Sample product transfer chaincode example", version = "0.0.1-SNAPSHOT"))

@Default
public final class ProductTransfer implements ContractInterface {

	private final Genson genson = new Genson();

	private enum ProductTransferErrors {
		PRODUCT_NOT_FOUND, PRODUCT_ALREADY_EXISTS, SUPPLY_NOT_ENOUGH
	}

	/**
	 * Add some initial properties to the ledger
	 *
	 * @param ctx the transaction context
	 */
	@Transaction()
	public void initLedger(final Context ctx) {

		ChaincodeStub stub = ctx.getStub();

		Product product = new Product("FirstOwner_FirstProduct_00.00.2000", "FirstProduct_00.00.2000", "FirstProduct",
				"FirstOwner", "10$", "70", "01.01.2199", "02.02.2020", "on sale", "00.00.2000", "FirstOwner", " ");

		String productState = genson.serialize(product);

		stub.putStringState("1", productState);
	}

	/**
	 * Add new product on the ledger.
	 *
	 * @param ctx              the transaction context
	 * @param id               the key for the new product
	 * @param productId        the id for the new product
	 * @param name             the name of the new product
	 * @param numberOf         the number of the new product
	 * @param ownername        the owner of the new product
	 * @param value            the value of the new product
	 * @param manufacturedDate the manufacturedDate of the new product
	 * @param expirationDate   the expirationDate of the new product
	 * @param issueDate        the issueDate of the new product
	 * @param supplier         the supplier of the new product
	 * @param demander         the demander of the new product
	 * @param status           the status of the new product, on sale or purchased
	 * @return the created product
	 */

	@Transaction()
	public Product addNewProduct(final Context ctx, final String id, final String productId, final String name,
			final String ownername, final String value, final String numberOf, final String expirationDate,
			final String manufacturedDate, final String status, final String issueDate, final String supplier,
			final String demander) {

		ChaincodeStub stub = ctx.getStub();

		String productState = stub.getStringState(id);

		if (!productState.isEmpty()) {
			String errorMessage = String.format("Product %s already exists", id);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage, ProductTransferErrors.PRODUCT_ALREADY_EXISTS.toString());
		}

		Product product = new Product(id, productId, name, ownername, value, numberOf, expirationDate, manufacturedDate,
				status, issueDate, supplier, demander);

		productState = genson.serialize(product);

		stub.putStringState(id, productState);

		return product;
	}

	/**
	 * Retrieves a product based upon product Id from the ledger.
	 *
	 * @param ctx the transaction context
	 * @param id  the key
	 * @return the product found on the ledger if there was one
	 */
	@Transaction()
	public Product queryProductById(final Context ctx, final String id) {
		ChaincodeStub stub = ctx.getStub();
		String productState = stub.getStringState(id);

		if (productState.isEmpty()) {
			String errorMessage = String.format("Product %s does not exist", id);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage, ProductTransferErrors.PRODUCT_NOT_FOUND.toString());
		}

		Product product = genson.deserialize(productState, Product.class);
		return product;
	}

	/**
	 * change product owner
	 *
	 * @param ctx      the transaction context
	 * @param id       the key
	 * @param newOwner the new owner
	 * @return the updated product
	 */
	@Transaction()
	public Product changeProductOwnership(final Context ctx, final String id, final String newProductOwner) {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		Date dateobj = new Date();
		ChaincodeStub stub = ctx.getStub();

		String productState = stub.getStringState(id);
		// Integer remaining = Integer.parseInt(supply.getNumberOf()) -
		// Integer.parseInt(numberOf);

		if (productState.isEmpty()) {
			String errorMessage = String.format("Product %s does not exist", id);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage, ProductTransferErrors.PRODUCT_NOT_FOUND.toString());
		}

		Product product = genson.deserialize(productState, Product.class);

		Product newProduct = new Product(product.getId(), product.getProductId(), product.getName(), newProductOwner,
				product.getValue(), product.getNumberOf(), product.getExpirationDate(), product.getManufacturedDate(),
				"changed", df.format(dateobj), product.getSupplier(), product.getDemander());

		String newProductState = genson.serialize(newProduct);
		stub.putStringState(id, newProductState);

		return newProduct;
	}

	/**
	 * Delete a product
	 *
	 * @param ctx the transaction context
	 * @param id  the key
	 * @return the updated product
	 */
	@Transaction()
	public String deleteProduct(final Context ctx, final String id) {
		ChaincodeStub stub = ctx.getStub();

		String productState = stub.getStringState(id);

		if (productState.isEmpty()) {
			String errorMessage = String.format("Product %s does not exist", id);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage, ProductTransferErrors.PRODUCT_NOT_FOUND.toString());
		}

		stub.delState(id);

		return "Product deleted successfully";
	}

	/**
	 * Subtract from product on sale on the ledger. And update the number of
	 * purchase product
	 *
	 * @param ctx      the transaction context
	 * @param id       the key
	 * @param newOwner the new owner
	 * @return the updated product
	 */
	@Transaction()
	public Product purchaseSomeProduct(final Context ctx, final String id, final String newProductOwner,
			final String numberOfPurchased) {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		Date dateobj = new Date();
		String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		ChaincodeStub stub = ctx.getStub();

		String productState = stub.getStringState(id);

		if (productState.isEmpty()) {
			String errorMessage = String.format("Product %s does not exist", id);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage, ProductTransferErrors.PRODUCT_NOT_FOUND.toString());
		}

		Product product = genson.deserialize(productState, Product.class);

		Integer remaining = Integer.parseInt(product.getNumberOf()) - Integer.parseInt(numberOfPurchased);

		Product newProduct = new Product(product.getId(), product.getProductId(), product.getName(), product.getOwner(),
				product.getValue(), remaining.toString(), product.getExpirationDate(), product.getManufacturedDate(),
				"on sale", timeStamp.toString(), product.getSupplier(), product.getDemander());

		if (remaining > 0) {// if the number of the supply is enough for purchase

			String newProductState = genson.serialize(newProduct); // update supply
			stub.putStringState(id, newProductState);

			Product newProduct2 = new Product(newProductOwner + "_" + product.getName() + "_" + product.getIssueDate(),
					product.getProductId(), product.getName(), newProductOwner, product.getValue(),
					numberOfPurchased.toString(), product.getExpirationDate(), product.getManufacturedDate(),
					"purchased", timeStamp.toString(), product.getOwner(), newProductOwner);

			String newProductState2 = genson.serialize(newProduct2); // create purchase
			stub.putStringState(id, newProductState2);

			return newProduct;

		} else if (remaining == 0) {// all product is purchased
			// delete supply

			stub.delState(id);

			Product newProduct2 = new Product(newProductOwner + "_" + product.getName() + "_" + product.getIssueDate(),
					product.getProductId(), product.getName(), newProductOwner, product.getValue(),
					numberOfPurchased.toString(), product.getManufacturedDate(), product.getExpirationDate(),
					"purchased", timeStamp.toString(), product.getOwner(), newProductOwner);

			String newProductState2 = genson.serialize(newProduct2); // create purchase
			stub.putStringState(id, newProductState2);

			return newProduct2;

		} else {// intended to be purchased product is more than supply
			String errorMessage = String.format("Supply %s is not enough", id);
			System.out.println(errorMessage);
			throw new ChaincodeException(errorMessage, ProductTransferErrors.SUPPLY_NOT_ENOUGH.toString());
		}

	}

	/*
	 * Minimize z = 41x1 + 35x2 +96x3
	 * 
	 * 2x1 + 3x2 + 7x3 >= 1250 1x1 + 1x2 + 0x3 >= 250 5x1 + 3x2 + 0x3 >= 900 0.6x1 +
	 * 0.25x2 + 1x3 >= 232.5 x1 >=0, x2>=0, x3 >=0
	 */
	/*
	 * int n = 3; int m = 4; double[] c = { 41, 35, 96 };
	 * 
	 * double[][] A = { { 2, 3, 7 }, { 1, 1, 0 }, { 5, 3, 0 }, { 0.6, 0.25, 1 } };
	 * 
	 * double[] b = { 1250, 250, 900, 232.5 };
	 * 
	 * solveModel(n, m, c, A, b);
	 */
	private double[] convertToDoubleArray(String s) {
		// 4,5,6
		String[] ls;
		ls = s.split(",");
		double[] d = new double[ls.length];
		for (int i = 0; i < ls.length; i++) {
			System.out.println(ls[i]);
			d[i] = Double.parseDouble(ls[i]);
		}
		return d;
	}

	private double[][] convertToDouble2DArray(String s) {
		// {{4, 2, 2, 4}, {3, 4, 5, 6}, {6, 7, 8, 9}, {3, 2, 1, 4}}
		s = s.replace("{", "");// replacing all [ to ""
		s = s.substring(0, s.length() - 2);// ignoring last two ]]
		String s1[] = s.split("},");// separating all by "],"

		String my_matrics[][] = new String[s1.length][s1.length];// declaring two dimensional matrix for input
		double[][] A = new double[s1.length][s1.length];
		for (int i = 0; i < s1.length; i++) {
			s1[i] = s1[i].trim();// ignoring all extra space if the string s1[i] has
			String single_int[] = s1[i].split(", ");// separating integers by ", "

			for (int j = 0; j < single_int.length; j++) {
				my_matrics[i][j] = single_int[j];// adding single values
				A[i][j] = Double.parseDouble(my_matrics[i][j]);
			}
		}

		return A;
	}

	@Transaction()
	public String solveModel(final Context ctx, final String _n, final String _m, final String _c, final String _A,
			final String _b) {

		int n = Integer.parseInt(_n);
		int m = Integer.parseInt(_m);
		double[] c = convertToDoubleArray(_c);
		double[][] A = convertToDouble2DArray(_A);
		double[] b = convertToDoubleArray(_b);

		try {

			@SuppressWarnings("resource")
			IloCplex model = new IloCplex();

			IloNumVar[] x = new IloNumVar[n];
			for (int i = 0; i < n; i++) {
				x[i] = model.numVar(0, Double.MAX_VALUE);
			}

			IloLinearNumExpr obj = model.linearNumExpr();
			for (int i = 0; i < n; i++) {
				obj.addTerm(c[i], x[i]);
			}
			model.addMinimize(obj);

			List<IloRange> constraints = new ArrayList<IloRange>();

			for (int i = 0; i < m; i++) {
				IloLinearNumExpr constraint = model.linearNumExpr();
				for (int j = 0; j < n; j++) {
					constraint.addTerm(A[i][j], x[j]);
				}
				constraints.add(model.addGe(constraint, b[i]));
			}

			boolean isSolved = model.solve();
			if (isSolved) {
				double objValue = model.getObjValue();
				System.out.println("onb_val = " + objValue);
				for (int k = 0; k < n; k++) {
					System.out.println("x[" + (k + 1) + "] = " + model.getValue(x[k]));
					System.out.println("Reduce cost " + (k + 1) + " = " + model.getReducedCost(x[k]));
				}

				for (int i = 0; i < m; i++) {

					double slack = model.getSlack(constraints.get(1));

					double dual = model.getDual(constraints.get(i));
					if (slack == 0) {
						System.out.println("Constraint " + (i + 1) + " is binding.");
					} else {
						System.out.println("Constraint " + (i + 1) + " is non-binding.");
					}

					System.out.println("Shadow price " + (i + 1) + " = " + dual);
				}
			} else {
				System.out.println("Model is not solved");
			}

		} catch (IloException ex) {
			ex.printStackTrace();
		}

		return "Model is not solved";
	}
}