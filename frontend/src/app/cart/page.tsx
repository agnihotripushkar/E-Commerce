'use client';

import Link from 'next/link';
import { useCart } from '@/context/CartContext';

const CATEGORY_EMOJI: Record<string, string> = {
  Mats: '🧘', Equipment: '💪', Apparel: '👕', Accessories: '🎒', Supplements: '🌿',
};

export default function CartPage() {
  const { items, removeItem, updateQuantity, total, clearCart } = useCart();

  if (items.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-24 text-center">
        <span className="text-6xl block mb-4">🛒</span>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Your cart is empty</h1>
        <p className="text-gray-500 mb-8">Add some yoga gear to get started!</p>
        <Link href="/products"
          className="inline-block bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-8 py-3 rounded-xl transition-colors">
          Browse Products
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Shopping Cart</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Items */}
        <div className="lg:col-span-2 space-y-4">
          {items.map(item => (
            <div key={item.product.id}
              className="bg-white rounded-2xl border border-gray-100 p-4 flex items-center gap-4">
              <div className="bg-gradient-to-br from-emerald-50 to-teal-100 rounded-xl w-20 h-20 flex items-center justify-center text-3xl flex-shrink-0">
                {CATEGORY_EMOJI[item.product.category] ?? '🛍️'}
              </div>

              <div className="flex-1 min-w-0">
                <Link href={`/products/${item.product.id}`}
                  className="font-semibold text-gray-900 hover:text-emerald-700 transition-colors line-clamp-1">
                  {item.product.name}
                </Link>
                <p className="text-sm text-gray-500">{item.product.category}</p>
                <p className="text-emerald-700 font-bold mt-1">${Number(item.product.price).toFixed(2)}</p>
              </div>

              <div className="flex items-center gap-2">
                <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden">
                  <button onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                    className="px-2.5 py-1.5 text-gray-600 hover:bg-gray-50 transition-colors">−</button>
                  <span className="px-3 py-1.5 text-sm font-medium min-w-8 text-center">{item.quantity}</span>
                  <button onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                    className="px-2.5 py-1.5 text-gray-600 hover:bg-gray-50 transition-colors">+</button>
                </div>
                <button onClick={() => removeItem(item.product.id)}
                  className="text-gray-400 hover:text-red-500 transition-colors p-1.5">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          ))}

          <button onClick={clearCart}
            className="text-sm text-gray-400 hover:text-red-500 transition-colors mt-2">
            Clear cart
          </button>
        </div>

        {/* Summary */}
        <div className="bg-white rounded-2xl border border-gray-100 p-6 h-fit sticky top-24">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Order Summary</h2>
          <div className="space-y-2 text-sm mb-4">
            {items.map(item => (
              <div key={item.product.id} className="flex justify-between text-gray-600">
                <span className="truncate max-w-[150px]">{item.product.name} × {item.quantity}</span>
                <span>${(Number(item.product.price) * item.quantity).toFixed(2)}</span>
              </div>
            ))}
          </div>
          <div className="border-t border-gray-100 pt-3 flex justify-between font-bold text-gray-900 text-lg mb-6">
            <span>Total</span>
            <span>${total.toFixed(2)}</span>
          </div>
          <Link href="/checkout"
            className="block w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-3 rounded-xl text-center transition-colors">
            Proceed to Checkout
          </Link>
          <Link href="/products"
            className="block w-full text-center text-sm text-gray-500 hover:text-emerald-700 mt-3 transition-colors">
            Continue Shopping
          </Link>
        </div>
      </div>
    </div>
  );
}
